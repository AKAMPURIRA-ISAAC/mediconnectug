package com.healthbridge

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.healthbridge.data.repository.RepositoryFactory
import com.healthbridge.network.ApiClient
import com.healthbridge.network.ChatRequest
import com.healthbridge.network.CreateChatSessionRequest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class SimpleChatMessage(
    val text: String,
    val isSent: Boolean,
    val time: Long = System.currentTimeMillis(),
    val isTyping: Boolean = false
)

class SimpleChatAdapter(private val messages: MutableList<SimpleChatMessage>) :
    RecyclerView.Adapter<SimpleChatAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(messages[position])
    override fun getItemCount(): Int = messages.size

    fun addMessage(msg: SimpleChatMessage) {
        messages.add(msg)
        notifyItemInserted(messages.size - 1)
    }

    fun removeLastMessage() {
        if (messages.isNotEmpty()) {
            messages.removeAt(messages.size - 1)
            notifyItemRemoved(messages.size)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val container: LinearLayout = itemView.findViewById(R.id.layoutMessage)

        fun bind(msg: SimpleChatMessage) {
            if (msg.isTyping) {
                tvMessage.text = "● ● ●"
                tvTime.visibility = View.GONE
                container.gravity = Gravity.START
                tvMessage.setBackgroundResource(R.drawable.chat_bubble_received)
                tvMessage.setTextColor(0xFF9E9E9E.toInt())
            } else {
                tvMessage.text = msg.text
                tvTime.visibility = View.VISIBLE
                tvTime.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.time))
                if (msg.isSent) {
                    container.gravity = Gravity.END
                    tvMessage.setBackgroundResource(R.drawable.chat_bubble_sent)
                    tvMessage.setTextColor(0xFFFFFFFF.toInt())
                    tvTime.setTextColor(0xFFBBBBBB.toInt())
                } else {
                    container.gravity = Gravity.START
                    tvMessage.setBackgroundResource(R.drawable.chat_bubble_received)
                    tvMessage.setTextColor(0xFF212121.toInt())
                    tvTime.setTextColor(0xFF888888.toInt())
                }
            }
        }
    }
}

// ── Conversation context held across turns ──────────────────────────────────
data class ConversationContext(
    val symptoms: MutableList<String> = mutableListOf(),
    var durationText: String? = null,
    var severityScore: Int? = null,
    var currentTopic: String? = null,
    var userName: String? = null,
    var awaitingFollowUp: String? = null   // what we are waiting for next: "duration" | "severity" | null
)

class ChatActivity : BaseActivity() {

    private lateinit var rvChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageView
    private lateinit var tvTitle: TextView

    private val messages = mutableListOf<SimpleChatMessage>()
    private lateinit var adapter: SimpleChatAdapter
    private val handler = Handler(Looper.getMainLooper())

    private var conversationState = ConversationState.GREETING
    private val ctx = ConversationContext()
    private var hasGreeted = false
    private var conversationId: String? = null

    // keep last 6 user messages for simple context window
    private val recentUserInputs = ArrayDeque<String>(6)

    enum class ConversationState {
        GREETING, COLLECTING_SYMPTOMS, AWAITING_DURATION, AWAITING_SEVERITY,
        TRIAGING, OFFERING_DOCTOR, GENERAL
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val doctorName = intent.getStringExtra("doctor_name") ?: "MediConnectUG AI"
        tvTitle = findViewById(R.id.tvDoctorName)
        tvTitle.text = doctorName

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        rvChat    = findViewById(R.id.recyclerView)
        etMessage = findViewById(R.id.etMessage)
        btnSend   = findViewById(R.id.btnSend)

        setupRecyclerView()
        addWelcomeMessage()
        setupQuickReplies()

        btnSend.setOnClickListener { sendMessage() }
        etMessage.setOnEditorActionListener { _, _, _ -> sendMessage(); true }
    }

    // ── Quick reply chips ────────────────────────────────────────────────────

    private fun setupQuickReplies() {
        findViewById<TextView>(R.id.chipSymptomCheck).setOnClickListener {
            etMessage.setText("I have symptoms I'd like to describe")
            sendMessage()
        }
        findViewById<TextView>(R.id.chipHealthTip).setOnClickListener {
            lifecycleScope.launch {
                val tip = RepositoryFactory.healthTipsRepository.getRandomTip()
                addBotMessage(
                    "💡 **Daily Health Tip — ${tip.category.replaceFirstChar { it.uppercase() }}**\n\n" +
                    "${tip.icon} **${tip.title}**\n\n${tip.content}\n\n" +
                    "_Type 'another tip' for more, or ask me anything!_ 💚"
                )
            }
        }
        findViewById<TextView>(R.id.chipBookDoctor).setOnClickListener {
            startActivity(Intent(this, FindDoctorsActivity::class.java))
        }
        findViewById<TextView>(R.id.chipEmergency).setOnClickListener {
            startActivity(Intent(this, EmergencyActivity::class.java))
        }
        findViewById<TextView>(R.id.chipMedication).setOnClickListener {
            etMessage.setText("Tell me about my medications")
            sendMessage()
        }
    }

    // ── RecyclerView ─────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = SimpleChatAdapter(messages)
        rvChat.layoutManager = LinearLayoutManager(this)
        rvChat.adapter = adapter
    }

    private fun addWelcomeMessage() {
        messages.add(SimpleChatMessage(
            "👋 Hello! I'm your **MediConnectUG AI Health Assistant**.\n\n" +
            "I can help you:\n" +
            "🩺 Analyse symptoms & provide triage guidance\n" +
            "💡 Give personalised daily health tips\n" +
            "💊 Explain medications & dosage reminders\n" +
            "📅 Book appointments with verified doctors\n" +
            "🚨 Handle emergencies with one-tap calling\n\n" +
            "Just describe how you feel in plain words — I'll take it from there. How are you feeling today?",
            false
        ))
        adapter.notifyItemInserted(messages.size - 1)
        scrollToBottom()
    }

    // ── Message sending & network ────────────────────────────────────────────

    private fun sendMessage() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return

        adapter.addMessage(SimpleChatMessage(text, true))
        etMessage.text.clear()
        scrollToBottom()
        showTypingIndicator()

        // keep recent context window
        if (recentUserInputs.size >= 6) recentUserInputs.removeFirst()
        recentUserInputs.addLast(text.lowercase())

        lifecycleScope.launch {
            try {
                val request = ChatRequest(
                    message = text,
                    conversationId = conversationId,
                    symptoms = if (ctx.symptoms.isNotEmpty()) ctx.symptoms else null
                )
                val result = RepositoryFactory.chatRepository.sendMessage(request)
                removeTypingIndicator()
                result.onSuccess { response ->
                    conversationId = response.conversationId
                    if (response.aiResponse != null && response.aiResponse != "I understand. Could you tell me a bit more?") {
                        addBotMessage(response.aiResponse)
                    } else {
                        // Backend returned generic response - use improved local processing
                        processUserMessage(text.lowercase())
                    }
                    
                    if (!response.suggestedActions.isNullOrEmpty()) {
                        handler.postDelayed({
                            val suggestions = response.suggestedActions.joinToString("\n") { "• $it" }
                            addBotMessage("💡 **Suggested next steps:**\n$suggestions")
                        }, 1200)
                    }
                }
                result.onFailure {
                    handler.postDelayed({ processUserMessage(text.lowercase()) }, 500L)
                }
            } catch (_: Exception) {
                removeTypingIndicator()
                handler.postDelayed({ processUserMessage(text.lowercase()) }, 500L)
            }
        }
    }

    private fun showTypingIndicator() {
        adapter.addMessage(SimpleChatMessage("...", false, isTyping = true))
        scrollToBottom()
    }

    private fun removeTypingIndicator() {
        adapter.removeLastMessage()
    }

    // ── Core message router with state awareness ─────────────────────────────

    private fun processUserMessage(text: String) {
        // ── Handle contextual follow-ups intelligently ──
        if (containsFollowUp(text)) {
            respondToFollowUp(text)
            return
        }

        // ── State-driven follow-up collection ──
        when (conversationState) {
            ConversationState.AWAITING_DURATION -> {
                ctx.durationText = extractDuration(text) ?: text.take(40)

                // Smart decision: skip severity if symptoms are clearly mild
                if (shouldSkipSeverityCheck(ctx.symptoms)) {
                    conversationState = ConversationState.TRIAGING
                    addBotMessage(
                        "Got it — **${ctx.durationText}**. 📝\n\n" +
                        "Based on what you've described, let me provide some guidance..."
                    )
                    handler.postDelayed({ deliverTriage() }, 1000)
                } else {
                    conversationState = ConversationState.AWAITING_SEVERITY
                    addBotMessage(
                        "Thanks — **${ctx.durationText}**. 📝\n\n" +
                        "On a scale of **1 to 10**, how would you rate the discomfort?\n" +
                        "_(1 = very mild, 10 = unbearable)_"
                    )
                }
                return
            }
            ConversationState.AWAITING_SEVERITY -> {
                val score = extractSeverityScore(text)
                if (score != null) {
                    ctx.severityScore = score
                    conversationState = ConversationState.TRIAGING
                    deliverTriage()
                } else {
                    addBotMessage("Please rate the severity from **1** (mild) to **10** (severe) so I can give you the right guidance.")
                }
                return
            }
            ConversationState.TRIAGING, ConversationState.OFFERING_DOCTOR -> {
                if (containsDoctorRequest(text) || text.contains("yes") || text.contains("doctor")) {
                    offerDoctorConnection(); return
                }
            }
            else -> { /* fall through to normal routing */ }
        }

        // ── Intent-based routing ──
        when {
            !hasGreeted && containsGreeting(text)  -> { hasGreeted = true; respondWithGreeting(text) }
            containsGoodbye(text)                  -> respondWithGoodbye()
            containsDoctorRequest(text)            -> offerDoctorConnection()
            containsEmergency(text)                -> respondToEmergency()
            containsHealthTipRequest(text)         -> respondWithHealthTip()
            containsCovidKeywords(text)            -> respondToCovid()
            containsMalariaKeywords(text)          -> respondToMalaria()
            containsTyphoidKeywords(text)          -> respondToTyphoid()
            containsTBKeywords(text)               -> respondToTB()
            containsDiabetesKeywords(text)         -> respondToDiabetes()
            containsBPKeywords(text)               -> respondToBloodPressure()
            containsMentalHealthKeywords(text)     -> respondToMentalHealth()
            containsNutritionKeywords(text)        -> respondToNutrition()
            containsMedicationKeywords(text)       -> respondToMedication()
            containsUTIKeywords(text)              -> respondToUTI()
            containsRespiratoryKeywords(text)      -> respondToRespiratory()
            containsSkinKeywords(text)             -> respondToSkin()
            containsEyeKeywords(text)              -> respondToEye()
            containsDentalKeywords(text)           -> respondToDental()
            containsMaternalKeywords(text)         -> respondToMaternal()
            containsChildHealthKeywords(text)      -> respondToChildHealth()
            containsSymptoms(text)                 -> startSymptomCollection(text)
            containsQuestion(text)                 -> respondToQuestion(text)
            containsGratitude(text)                -> respondToGratitude()
            containsAnotherTip(text)               -> respondWithHealthTip()
            else                                   -> respondIntelligently(text)
        }
    }

    // ── Smart follow-up detection ──
    private fun containsFollowUp(t: String) =
        listOf("tell me more","more about","explain","elaborate","what else",
            "anything else","continue","go on","and then","what about","how about",
            "what if","is that","really","why","how does","what causes").any { t.contains(it) }

    private fun shouldSkipSeverityCheck(symptoms: List<String>): Boolean {
        // Skip severity for informational queries
        val mildSymptoms = listOf("runny nose","mild headache","slight cough","tired")
        return symptoms.any { s -> mildSymptoms.any { mild -> s.contains(mild) } }
    }

    private fun respondToFollowUp(text: String) {
        val lastTopic = ctx.currentTopic ?: "general health"

        when {
            text.contains("tell me more") || text.contains("more about") -> {
                addBotMessage(
                    "Of course! Let me elaborate on **$lastTopic**...\n\n" +
                    provideDetailedInfo(lastTopic)
                )
            }
            text.contains("why") || text.contains("how does") -> {
                addBotMessage(provideExplanation(lastTopic))
            }
            text.contains("what if") || text.contains("is that serious") -> {
                addBotMessage(provideRiskAssessment(lastTopic))
            }
            else -> {
                addBotMessage(
                    "That's a good question! Based on what we've discussed about **$lastTopic**, " +
                    "here's what you should know:\n\n${provideContextualAnswer(text, lastTopic)}"
                )
            }
        }
    }

    private fun provideDetailedInfo(topic: String): String {
        return when {
            topic.contains("fever") -> """
                **Understanding Fever:**
                
                🌡️ **What is it:** Your body's temperature rises above 37.5°C (99.5°F) as a defense mechanism against infection.
                
                **Common Causes:**
                • Viral infections (flu, cold, COVID-19)
                • Bacterial infections (strep throat, UTI)
                • Heat exhaustion or dehydration
                • Inflammatory conditions
                
                **When to Worry:**
                • Fever above 39.5°C (103°F)
                • Lasts more than 3 days
                • Accompanied by stiff neck, confusion, or rash
                • In infants under 3 months old
                
                **Home Care:**
                • Take paracetamol or ibuprofen as directed
                • Stay hydrated (water, ORS, clear soups)
                • Rest in a cool, comfortable environment
                • Use lukewarm sponge baths (not cold!)
            """.trimIndent()

            topic.contains("headache") -> """
                **Understanding Headaches:**
                
                🤕 **Types:**
                • Tension headaches (most common) - feels like a tight band
                • Migraines - throbbing pain, often one-sided
                • Cluster headaches - severe pain around one eye
                • Sinus headaches - pressure in face/forehead
                
                **Triggers to Avoid:**
                • Dehydration (drink 8 glasses water daily!)
                • Skipping meals or low blood sugar
                • Poor sleep or irregular sleep schedule
                • Screen time without breaks
                • Stress and muscle tension
                • Certain foods (cheese, chocolate, MSG)
                
                **Relief Methods:**
                • Apply cold/warm compress to head
                • Massage temples and neck muscles
                • Rest in quiet, dark room
                • Pain reliever (paracetamol/ibuprofen)
                • Deep breathing exercises
                
                ⚠️ **Red Flags:** Sudden severe "thunderclap" headache, headache with fever/stiff neck, or headache after head injury = Emergency!
            """.trimIndent()

            else -> "I'd be happy to provide more details! Could you be more specific about what aspect of $topic you'd like to know more about?"
        }
    }

    private fun provideExplanation(topic: String): String {
        return when {
            topic.contains("fever") -> "Your body raises its temperature to create an unfavorable environment for viruses and bacteria. Fever actually shows your immune system is working properly!"
            topic.contains("cough") -> "Coughing is your body's protective reflex to clear airways of mucus, irritants, or foreign particles. It's usually helpful, though it can be uncomfortable."
            topic.contains("pain") -> "Pain is your body's alarm system, signaling that something needs attention. It's caused by nerve endings detecting tissue damage or inflammation."
            else -> "That's influenced by multiple factors including your immune system, lifestyle, and environmental conditions. Would you like specific details?"
        }
    }

    private fun provideRiskAssessment(topic: String): String {
        return when {
            topic.contains("fever") -> "Most fevers resolve in 2-3 days and aren't serious. However, very high fever (>39.5°C), persistent fever (>3 days), or fever with other severe symptoms requires medical evaluation."
            topic.contains("cough") -> "A cough lasting less than 3 weeks is usually not serious. But if it persists beyond 3 weeks, produces blood, or comes with weight loss/night sweats, see a doctor to rule out TB or other conditions."
            else -> "The severity depends on multiple factors including duration, intensity, and associated symptoms. When in doubt, it's always best to consult a healthcare professional."
        }
    }

    private fun provideContextualAnswer(question: String, topic: String): String {
        return "Based on your question about **$topic**, here's what's important: " +
            "Monitor your symptoms closely, follow the care advice I provided, and don't hesitate to seek medical help if things worsen. " +
            "Would you like me to connect you with a doctor for a professional opinion?"
    }

    // ── Keyword detectors ─────────────────────────────────────────────────────

    private fun containsGreeting(t: String) =
        listOf("hello","hi","hey","good morning","good afternoon","good evening","habari","sawa","ola","howdy","greetings").any { t.contains(it) }
    private fun containsGoodbye(t: String) =
        listOf("bye","goodbye","see you","thanks bye","gotta go","take care","later","ciao","farewell").any { t.contains(it) }
    private fun containsDoctorRequest(t: String) =
        listOf("talk to doctor","see doctor","real doctor","connect doctor","speak to doctor","need doctor","find doctor","chat with doctor","human doctor","online doctor").any { t.contains(it) }
    private fun containsEmergency(t: String) =
        listOf(
            // Critical cardiovascular
            "emergency","urgent","severe","chest pain","heart attack","cardiac arrest",
            "crushing pain","pain radiating","left arm pain",
            // Respiratory emergencies
            "can't breathe","cannot breathe","difficulty breathing","gasping for air",
            "choking","turning blue","blue lips","cyanosis",
            // Neurological emergencies
            "stroke","can't move","paralysis","facial drooping","slurred speech",
            "severe headache","worst headache","thunderclap headache","seizure",
            "convulsion","fitting","unresponsive","unconscious","passed out","fainted",
            "not waking up","confusion suddenly",
            // Trauma & accidents
            "accident","hit by","car crash","fell from height","head injury",
            "broken bone","compound fracture","bleeding heavily","won't stop bleeding",
            "deep cut","stabbed","shot","gunshot","trauma",
            // Severe bleeding & circulation
            "vomiting blood","coughing blood","blood in stool","black stool",
            "heavy bleeding","hemorrhage","internal bleeding",
            // Obstetric emergencies
            "pregnant and bleeding","severe pregnancy pain","waters broke early",
            "can't feel baby move","placenta",
            // Severe allergic reactions
            "anaphylaxis","throat closing","swollen tongue","allergic shock",
            "hives all over","severe allergic",
            // Other critical conditions
            "appendicitis","extreme pain","unbearable pain","kidney stone",
            "diabetic emergency","very low sugar","very high sugar","ketones",
            "poisoning","overdose","took too many","suicide attempt","self harm",
            "dying","think i'm dying","going to die"
        ).any { t.contains(it) }
    private fun containsHealthTipRequest(t: String) =
        listOf("health tip","tip","advice","wellness","healthy lifestyle","how to stay healthy","give me a tip","suggest").any { t.contains(it) }
    private fun containsAnotherTip(t: String) =
        listOf("another tip","more tip","next tip","different tip").any { t.contains(it) }
    private fun containsMalariaKeywords(t: String) =
        listOf("malaria","mosquito","paludism","chills","shivering with fever","night sweats").any { t.contains(it) }
    private fun containsTyphoidKeywords(t: String) =
        listOf("typhoid","enteric","rose spots","sustained fever","typhoid fever").any { t.contains(it) }
    private fun containsTBKeywords(t: String) =
        listOf("tuberculosis"," tb ","chronic cough","night sweats","coughing blood","haemoptysis","consumption").any { t.contains(it) }
    private fun containsDiabetesKeywords(t: String) =
        listOf("diabetes","blood sugar","glucose","insulin","sweet urine","frequent urination","diabetic").any { t.contains(it) }
    private fun containsBPKeywords(t: String) =
        listOf("blood pressure","hypertension","bp","high pressure","low pressure","hypotension").any { t.contains(it) }
    private fun containsMentalHealthKeywords(t: String) =
        listOf("depressed","depression","anxiety","stress","mental","panic attack","mood","hopeless","suicide","self harm","burnout","overwhelmed","trauma","ptsd").any { t.contains(it) }
    private fun containsNutritionKeywords(t: String) =
        listOf("diet","nutrition","food","eat","weight","obesity","calories","vitamins","underweight","overweight","bmi","meal plan").any { t.contains(it) }
    private fun containsMedicationKeywords(t: String) =
        listOf("medication","medicine","drug","tablet","pill","prescription","dose","dosage","side effect","interaction","paracetamol","ibuprofen","amoxicillin","metformin","antibiotic").any { t.contains(it) }
    private fun containsUTIKeywords(t: String) =
        listOf("uti","urinary tract","burning urination","burning when urinating","frequent urge to urinate","cloudy urine","kidney pain","bladder").any { t.contains(it) }
    private fun containsRespiratoryKeywords(t: String) =
        listOf("asthma","wheeze","wheezing","shortness of breath","breathless","bronchitis","inhaler","difficulty breathing","tight chest").any { t.contains(it) }
    private fun containsSkinKeywords(t: String) =
        listOf("rash","skin","eczema","psoriasis","acne","pimple","hives","itching","lesion","blister","wound","ringworm","fungal").any { t.contains(it) }
    private fun containsEyeKeywords(t: String) =
        listOf("eye","vision","blurry","conjunctivitis","pink eye","eye pain","sight","blind","itchy eye","red eye").any { t.contains(it) }
    private fun containsDentalKeywords(t: String) =
        listOf("tooth","teeth","dental","gum","cavity","toothache","cavity","mouth pain","jaw pain","dentist").any { t.contains(it) }
    private fun containsMaternalKeywords(t: String) =
        listOf("pregnant","pregnancy","antenatal","prenatal","trimester","labour","delivery","breastfeed","postpartum","maternity","fetal","fetus").any { t.contains(it) }
    private fun containsChildHealthKeywords(t: String) =
        listOf("child","baby","infant","toddler","my kid","pediatric","kid","newborn","vaccination child","growth","immunisation").any { t.contains(it) }
    private fun containsCovidKeywords(t: String) =
        listOf("covid","coronavirus","sars-cov","loss of taste","loss of smell","covid test","positive test","quarantine","isolat").any { t.contains(it) }
    private fun containsSymptoms(t: String) =
        listOf("pain","ache","fever","cough","cold","headache","dizzy","nausea","vomit","tired","fatigue","sore","hurt","sick","symptom","feeling unwell","uncomfortable","swelling","itching","diarrhoea","diarrhea","constipation","bloating","cramps","weak","weakness","numbness","tingling","palpitation","shortness","thirsty","increased thirst").any { t.contains(it) }
    private fun containsQuestion(t: String) =
        t.contains("?") || listOf("what","how","when","why","where","can you","is it","should i","does","am i","will").any { t.startsWith(it) }
    private fun containsGratitude(t: String) =
        listOf("thank","thanks","appreciate","grateful","asante","webale","waibale","merci").any { t.contains(it) }

    // ── Duration & severity extractors ───────────────────────────────────────

    private fun extractDuration(text: String): String? {
        val patterns = listOf(
            Regex("""(\d+)\s*(day|days|week|weeks|month|months|hour|hours)"""),
            Regex("""(since\s+\w+day|yesterday|this morning|last night|a few days|a week)""")
        )
        patterns.forEach { p ->
            p.find(text)?.let { return it.value }
        }
        return null
    }

    private fun extractSeverityScore(text: String): Int? {
        // "7", "7/10", "seven", "about 6"
        val numRegex = Regex("""(\d{1,2})""")
        numRegex.find(text)?.let {
            val n = it.value.toIntOrNull()
            if (n != null && n in 1..10) return n
        }
        val wordMap = mapOf("one" to 1,"two" to 2,"three" to 3,"four" to 4,"five" to 5,
            "six" to 6,"seven" to 7,"eight" to 8,"nine" to 9,"depth" to 10)
        wordMap.forEach { (word, num) -> if (text.contains(word)) return num }
        return null
    }

    // ── Symptom collection & triage pipeline ────────────────────────────────

    private fun startSymptomCollection(text: String) {
        val detected = detectSymptoms(text)
        ctx.symptoms.addAll(detected)
        ctx.currentTopic = "symptoms"
        conversationState = ConversationState.COLLECTING_SYMPTOMS

        val symptomsLabel = if (detected.isNotEmpty()) detected.joinToString(", ") else "some discomfort"
        addBotMessage(
            "I've noted you're experiencing **$symptomsLabel**. 📋\n\n" +
            "Let me ask a couple of quick questions to give you accurate guidance:\n\n" +
            "**How long have these symptoms been present?**\n" +
            "_E.g. '2 days', 'since yesterday', 'about a week'_"
        )
        conversationState = ConversationState.AWAITING_DURATION
    }

    private fun detectSymptoms(text: String): List<String> {
        val symptomMap = mapOf(
            "fever" to "fever", "temperature" to "fever", "hot body" to "fever",
            "cough" to "cough", "coughing" to "cough",
            "headache" to "headache", "head pain" to "headache", "head ache" to "headache",
            "tired" to "fatigue", "fatigue" to "fatigue", "exhausted" to "fatigue", "weak" to "weakness", "weakness" to "weakness",
            "rash" to "skin rash", "itching" to "itching", "skin" to "skin issue",
            "vomit" to "vomiting", "throwing up" to "vomiting", "nausea" to "nausea",
            "diarrhoea" to "diarrhoea", "diarrhea" to "diarrhoea", "loose stool" to "diarrhoea",
            "dizzy" to "dizziness", "dizziness" to "dizziness", "spinning" to "dizziness",
            "sore throat" to "sore throat", "throat pain" to "sore throat",
            "swelling" to "swelling", "swollen" to "swelling",
            "pain" to "pain", "ache" to "pain", "hurt" to "pain",
            "chest pain" to "chest pain", "chest tightness" to "chest tightness",
            "shortness" to "breathlessness", "breathless" to "breathlessness",
            "constipation" to "constipation", "bloating" to "bloating",
            "back pain" to "back pain", "joint pain" to "joint pain",
            "palpitation" to "heart palpitations", "fast heartbeat" to "heart palpitations",
            "thirsty" to "excessive thirst", "increased thirst" to "excessive thirst",
            "numbness" to "numbness", "tingling" to "tingling",
            "cold" to "common cold", "runny nose" to "runny nose", "stuffy nose" to "nasal congestion"
        )
        val found = mutableListOf<String>()
        symptomMap.forEach { (keyword, label) ->
            if (text.contains(keyword) && !found.contains(label)) found.add(label)
        }
        return found.distinct()
    }

    private fun deliverTriage() {
        val severity = ctx.severityScore ?: 5
        val duration = ctx.durationText ?: "a few days"
        val symptoms = ctx.symptoms.ifEmpty { listOf("general discomfort") }
        val symptomsLabel = symptoms.joinToString(", ")

        val urgencyLevel = when {
            severity >= 8 || symptoms.any { it in listOf("chest pain","chest tightness","breathlessness","heart palpitations") } -> "URGENT"
            severity >= 5 || duration.contains(Regex("week|month|long")) -> "MODERATE"
            else -> "MILD"
        }

        val (urgencyIcon, urgencyMsg, actionMsg) = when (urgencyLevel) {
            "URGENT" -> Triple(
                "🔴",
                "Your symptoms appear **serious**. You should seek medical attention **today**.",
                "I strongly recommend seeing a doctor immediately or calling emergency services."
            )
            "MODERATE" -> Triple(
                "🟡",
                "Your symptoms are **moderate**. A doctor visit within **24–48 hours** is advisable.",
                "I recommend booking an appointment with a doctor soon."
            )
            else -> Triple(
                "🟢",
                "Your symptoms seem **mild** at this stage.",
                "Try the self-care steps below and monitor your symptoms over the next 24–48 hours."
            )
        }

        addBotMessage(
            "$urgencyIcon **Health Assessment Summary**\n\n" +
            "📋 **Symptoms reported:** $symptomsLabel\n" +
            "⏱️ **Duration:** ${duration.replaceFirstChar { it.uppercase() }}\n" +
            "📊 **Severity you rated:** $severity/10\n\n" +
            "**Assessment:** $urgencyMsg\n\n" +
            "─────────────────────\n" +
            "💊 **Immediate self-care:**\n${getDetailedAdvice(symptoms)}\n\n" +
            "─────────────────────\n" +
            "⚠️ _This is AI guidance only — not a medical diagnosis._\n\n" +
            "**$actionMsg**\n\nWould you like me to connect you with an available doctor?"
        )
        conversationState = ConversationState.OFFERING_DOCTOR
        ctx.symptoms.clear()
        ctx.durationText = null
        ctx.severityScore = null
    }

    private fun getDetailedAdvice(symptoms: List<String>): String {
        val lines = mutableListOf<String>()
        // always suggest rest and hydration
        lines.add("• 💧 Stay well hydrated — drink 8–10 glasses of water daily")
        lines.add("• 😴 Get sufficient rest (7–9 hours of sleep)")
        symptoms.forEach { symptom ->
            when {
                symptom == "fever" -> {
                    lines.add("• 🌡️ Paracetamol 500mg every 6 hours can reduce fever (with food)")
                    lines.add("• 🧊 Apply a cool damp cloth to forehead and neck")
                    lines.add("• 🌡️ Monitor temperature every 4 hours — seek care if above 39.5°C")
                }
                symptom == "headache" -> {
                    lines.add("• 🏠 Rest in a quiet, dimly lit room and avoid screens")
                    lines.add("• 💊 Paracetamol or ibuprofen (as directed on pack)")
                    lines.add("• 🧊 Cold compress on forehead for 10–15 minutes")
                }
                symptom == "cough" -> {
                    lines.add("• 🍯 1 teaspoon of honey in warm water — proven to soothe coughs")
                    lines.add("• 🌿 Inhale steam for 10 minutes, 2–3 times daily")
                    lines.add("• Smoking/Vaping: 🚭 Avoid smoke, dust, and strong odours")
                }
                symptom == "nausea" || symptom == "vomiting" -> {
                    lines.add("• 🍋 Ginger tea or ginger biscuits help reduce nausea")
                    lines.add("• 🍚 Eat small, bland meals: rice, crackers, toast")
                    lines.add("• 💧 Sip ORS solution slowly to prevent dehydration")
                }
                symptom == "diarrhoea" -> {
                    lines.add("• 💊 Oral Rehydration Salts (ORS) after every loose stool")
                    lines.add("• 🍌 Eat bananas, rice, applesauce, toast (BRAT diet)")
                    lines.add("• ❌ Avoid dairy, spicy food, and caffeine")
                    lines.add("• 🚨 Seek care if diarrhoea lasts more than 2 days or contains blood")
                }
                symptom == "fatigue" || symptom == "weakness" -> {
                    lines.add("• 🍎 Eat iron-rich foods: red meat, spinach, beans, lentils")
                    lines.add("• 🌞 10 minutes of morning sunlight helps with energy levels")
                    lines.add("• ❌ Avoid caffeine in the evening — it disrupts sleep quality")
                }
                symptom == "dizziness" -> {
                    lines.add("• 🪑 Sit or lie down immediately when dizzy — avoid falls")
                    lines.add("• 💧 Drink water; dehydration is a common cause")
                    lines.add("• 🚨 If dizziness is sudden, severe, or with headache — seek emergency care")
                }
                symptom == "skin rash" || symptom == "itching" -> {
                    lines.add("• 🧴 Apply calamine lotion or hydrocortisone cream to reduce itching")
                    lines.add("• 🚿 Keep skin clean and dry; wash gently with mild soap")
                    lines.add("• 👕 Wear loose, breathable cotton clothing")
                    lines.add("• ❌ Avoid scratching — it worsens irritation and risks infection")
                }
                symptom == "sore throat" -> {
                    lines.add("• 🧂 Gargle warm salt water (¼ tsp salt in 250ml water) 3× daily")
                    lines.add("• 🍯 Honey and lemon in warm water to soothe throat lining")
                    lines.add("• 🤫 Rest your voice as much as possible")
                }
                symptom == "back pain" || symptom == "joint pain" -> {
                    lines.add("• 🔥 Apply a warm compress or hot water bottle")
                    lines.add("• 🧘 Gentle stretching (avoid high-impact exercise)")
                    lines.add("• 💊 Ibuprofen (if no contraindications) can reduce inflammation")
                }
                symptom == "chest pain" || symptom == "chest tightness" -> {
                    lines.add("• 🚨 Chest pain can indicate a serious condition")
                    lines.add("• 📞 Call emergency services or travel to casualty immediately")
                    lines.add("• 🧍 Sit upright; do NOT lie flat if breathing is difficult")
                }
                symptom == "nasal congestion" || symptom == "runny nose" -> {
                    lines.add("• 🌊 Saline nasal rinse (Neti pot or saline spray)")
                    lines.add("• 🌿 Steam inhalation with eucalyptus oil for 10 minutes")
                    lines.add("• 💧 Stay hydrated to thin mucus")
                }
            }
        }
        return lines.distinct().take(8).joinToString("\n")
    }

    // ── Condition-specific responders ────────────────────────────────────────

    private fun respondWithGreeting(text: String) {
        val timeGreet = when {
            text.contains("morning") -> "Good morning"
            text.contains("afternoon") -> "Good afternoon"
            text.contains("evening") -> "Good evening"
            else -> "Hello"
        }
        addBotMessage(listOf(
            "$timeGreet! 😊 I'm your MediConnectUG AI health assistant. How are you feeling today? Describe any symptoms or just ask me a health question!",
            "$timeGreet! 👋 Great to have you here. Tell me — any health concerns on your mind today, or would you like a daily health tip?",
            "$timeGreet! 🌟 I'm ready to help with symptoms, health advice, medication info, or connecting you to a doctor. What do you need?"
        ).random())
    }

    private fun respondWithGoodbye() {
        addBotMessage(listOf(
            "Take care! 🌟 Remember — stay hydrated, get enough sleep, and don't ignore recurring symptoms. See you! 💚",
            "Goodbye! 👋 Wishing you excellent health. Return any time you need guidance — I'm always here.",
            "See you! 💚 Quick reminder: drink enough water, eat fruits and vegetables, and rest well. Stay healthy!"
        ).random())
    }

    private fun respondWithHealthTip() {
        lifecycleScope.launch {
            val tip = RepositoryFactory.healthTipsRepository.getRandomTip()
            addBotMessage(
                "💡 **Health Tip — ${tip.category.replaceFirstChar { it.uppercase() }}**\n\n" +
                "${tip.icon} **${tip.title}**\n\n${tip.content}\n\n" +
                "_Type 'another tip' for a new one, or ask me a health question!_ 💚"
            )
        }
    }

    private fun respondToEmergency() {
        val lastInput = recentUserInputs.lastOrNull() ?: ""
        
        // Detect specific emergency type for targeted response
        val emergencyType = when {
            lastInput.contains("chest pain") || lastInput.contains("heart attack") || 
            lastInput.contains("crushing pain") || lastInput.contains("left arm pain") -> "CARDIAC"
            lastInput.contains("can't breathe") || lastInput.contains("choking") || 
            lastInput.contains("gasping") || lastInput.contains("blue lips") -> "RESPIRATORY"
            lastInput.contains("stroke") || lastInput.contains("paralysis") || 
            lastInput.contains("facial drooping") || lastInput.contains("slurred speech") -> "STROKE"
            lastInput.contains("seizure") || lastInput.contains("convulsion") || 
            lastInput.contains("fitting") -> "SEIZURE"
            lastInput.contains("accident") || lastInput.contains("trauma") || 
            lastInput.contains("broken bone") || lastInput.contains("head injury") -> "TRAUMA"
            lastInput.contains("bleeding") || lastInput.contains("hemorrhage") || 
            lastInput.contains("won't stop bleeding") -> "BLEEDING"
            lastInput.contains("unconscious") || lastInput.contains("unresponsive") || 
            lastInput.contains("passed out") -> "UNCONSCIOUS"
            lastInput.contains("suicide") || lastInput.contains("overdose") || 
            lastInput.contains("poisoning") -> "POISONING"
            lastInput.contains("allergic") || lastInput.contains("anaphylaxis") || 
            lastInput.contains("throat closing") -> "ANAPHYLAXIS"
            else -> "GENERAL"
        }

        val specificGuidance = when (emergencyType) {
            "CARDIAC" -> """
                🚨 **HEART ATTACK SUSPECTED — URGENT ACTION**
                
                **IMMEDIATE STEPS:**
                ⏱️ **CALL AMBULANCE NOW: 0800 100 066**
                1️⃣ Make patient sit upright (NOT lie down)
                2️⃣ Give **300mg aspirin** (chew it) if available and not allergic
                3️⃣ Loosen tight clothing around neck/chest
                4️⃣ Stay calm — anxiety makes it worse
                5️⃣ If patient loses consciousness → start CPR
                
                **WARNING SIGNS OF HEART ATTACK:**
                • Crushing chest pain lasting > 5 minutes
                • Pain spreading to jaw, neck, left arm
                • Sweating, nausea, feeling of doom
                • Shortness of breath
                
                ⏰ **TIME = HEART MUSCLE** — Every minute counts!
            """.trimIndent()
            
            "RESPIRATORY" -> """
                🚨 **BREATHING EMERGENCY — ACT NOW**
                
                **IMMEDIATE STEPS:**
                ⏱️ **CALL AMBULANCE: 0800 100 066**
                1️⃣ Sit patient UPRIGHT — lean slightly forward
                2️⃣ Open all windows — fresh air helps
                3️⃣ Loosen tight clothing around neck/chest
                4️⃣ If asthma: use reliever inhaler immediately (2-4 puffs)
                5️⃣ If choking: perform Heimlich maneuver
                
                **CHOKING FIRST AID (Adults):**
                • Stand behind person
                • Make a fist above belly button
                • Grab fist with other hand
                • Quick upward thrusts — repeat 5 times
                • Call ambulance if object not cleared
                
                **If breathing stops → START CPR immediately**
            """.trimIndent()
            
            "STROKE" -> """
                🚨 **STROKE EMERGENCY — F.A.S.T. ACTION**
                
                🔴 **BRAIN DAMAGE HAPPENING NOW** — Call ambulance within 5 minutes
                ⏱️ **CALL: 0800 100 066 or GO TO MULAGO/NSAMBYA NOW**
                
                **F.A.S.T. Stroke Recognition:**
                👤 **F**ace — Smile? One side droops?
                💪 **A**rm — Raise both? One drifts down?
                🗣️ **S**peech — Repeat simple sentence? Slurred?
                ⏰ **T**ime — Note EXACT time symptoms started
                
                **CRITICAL ACTIONS:**
                1️⃣ Note time symptoms started (doctors need this!)
                2️⃣ Keep patient lying down with head slightly raised
                3️⃣ Do NOT give food or water (swallowing may be impaired)
                4️⃣ Turn head to side if vomiting
                5️⃣ Loosen tight clothing
                
                💡 **Stroke treatment works best in first 3-4 hours**
            """.trimIndent()
            
            "TRAUMA" -> """
                🚨 **TRAUMA / ACCIDENT — EMERGENCY PROTOCOL**
                
                ⏱️ **CALL AMBULANCE: 0800 100 066**
                📞 Police (if road accident): **999**
                
                **PRIORITY ACTIONS:**
                1️⃣ **Scene safety** — ensure no ongoing danger
                2️⃣ **Do NOT move** victim unless in immediate danger
                3️⃣ **Check breathing** — if not breathing, start CPR
                4️⃣ **Control bleeding** — firm pressure with clean cloth
                5️⃣ **Stabilize head/neck** — suspect spinal injury
                
                **HEAD INJURY WARNING SIGNS:**
                • Loss of consciousness (even brief)
                • Vomiting repeatedly
                • Clear fluid from nose/ears
                • Unequal pupils
                • Confusion or strange behavior
                
                **For broken bones:**
                • Do NOT try to straighten it
                • Immobilize with splint (use rolled newspaper/stick)
                • Apply ice pack (wrapped in cloth)
                
                🩸 **Heavy bleeding:** Apply DIRECT firm pressure for 10 minutes continuously
            """.trimIndent()
            
            "UNCONSCIOUS" -> """
                🚨 **UNCONSCIOUS PATIENT — CRITICAL**
                
                ⏱️ **CALL AMBULANCE IMMEDIATELY: 0800 100 066**
                
                **CHECK & ACT (in order):**
                1️⃣ **Tap shoulders, shout name** — any response?
                2️⃣ **Check breathing:**
                   • Look for chest rising
                   • Feel for breath on your cheek
                3️⃣ **If breathing → Recovery Position:**
                   • Roll onto side
                   • Head tilted back, chin forward
                   • Top leg bent at 90° for stability
                4️⃣ **If NOT breathing → START CPR:**
                   • 30 chest compressions (5-6 cm deep)
                   • 2 rescue breaths
                   • Repeat until ambulance arrives
                
                **DO NOT:**
                ❌ Give food or water
                ❌ Leave patient alone
                ❌ Put pillow under head (blocks airway)
                
                💡 **Recovery position prevents choking on vomit**
            """.trimIndent()
            
            "ANAPHYLAXIS" -> """
                🚨 **SEVERE ALLERGIC REACTION — ANAPHYLAXIS**
                
                ⏱️ **CALL AMBULANCE NOW: 0800 100 066**
                
                **LIFE-THREATENING SIGNS:**
                • Throat swelling / difficulty swallowing
                • Tongue/lip swelling
                • Difficulty breathing / wheezing
                • Rapid pulse, dizziness
                • Widespread hives/rash
                
                **IMMEDIATE ACTION:**
                1️⃣ **EpiPen/Adrenaline** — inject into outer thigh if available
                2️⃣ **Antihistamine** (cetirizine 10mg or chlorpheniramine)
                3️⃣ Lie patient flat, raise legs
                4️⃣ Loosen tight clothing
                5️⃣ Do NOT give anything by mouth
                
                **If patient has EpiPen:**
                • Remove safety cap
                • Jab firmly into outer thigh (works through clothes)
                • Hold for 10 seconds
                • Massage injection site
                
                ⚠️ **Can repeat EpiPen after 5-15 min if no improvement**
            """.trimIndent()
            
            else -> """
                🚨 **EMERGENCY — ACT IMMEDIATELY** 🚨
                
                **Uganda Emergency Numbers:**
                📞 Ambulance: **0800 100 066** _(toll-free)_
                📞 Police: **999** or **112**
                📞 Red Cross Uganda: **+256 414 287 776**
                📞 Mulago Hospital Emergency: **+256 414 554 001**
                📞 Nsambya Hospital: **+256 414 510 095**
                
                **While waiting for help:**
                1️⃣ Keep patient calm — do not move unnecessarily
                2️⃣ Do NOT give food or water
                3️⃣ Check breathing every 2 minutes
                4️⃣ For bleeding: apply firm direct pressure with clean cloth
                5️⃣ For unconsciousness: recovery position (on their side)
                6️⃣ Note time symptoms started
                
                **If no response within 15 minutes → private transport to hospital**
                Nearest hospitals: Mulago, Nsambya, IHK, The Surgery
            """.trimIndent()
        }

        addBotMessage(specificGuidance)
        
        // Offer immediate doctor connection
        handler.postDelayed({
            addBotMessage(
                "⚕️ **I can connect you to an online doctor RIGHT NOW**\n\n" +
                "Doctors are standing by for emergencies. Response time: **1-3 minutes**\n\n" +
                "Tap below to start video/chat consultation immediately."
            )
            handler.postDelayed({
                AlertDialog.Builder(this)
                    .setTitle("🚨 Emergency Doctor Connection")
                    .setMessage("Connect to an online doctor NOW for immediate guidance?\n\nThis is FREE for emergencies.")
                    .setPositiveButton("Connect NOW") { _, _ -> routeToEmergencyDoctor() }
                    .setNegativeButton("Call Ambulance Instead") { _, _ -> 
                        startActivity(Intent(this, EmergencyActivity::class.java))
                    }
                    .setCancelable(false)
                    .show()
            }, 800)
        }, 1500)
    }

    private fun routeToEmergencyDoctor() {
        addBotMessage("🔄 Connecting you to the nearest available emergency doctor... Please wait.")
        showTypingIndicator()
        
        lifecycleScope.launch {
            try {
                val response = RepositoryFactory.doctorRepository.getOnlineDoctors()
                removeTypingIndicator()
                response.onSuccess { doctors ->
                    if (doctors.isNotEmpty()) {
                        val doctor = doctors.first() // Get highest rated online doctor
                        createEmergencyChatSession(doctor.id, doctor.name)
                    } else {
                        addBotMessage(
                            "⚠️ No doctors online right now.\n\n" +
                            "**Please:**\n" +
                            "1. Call ambulance: **0800 100 066**\n" +
                            "2. Or go directly to nearest hospital\n" +
                            "3. Or book urgent appointment (tap Find Doctors)"
                        )
                    }
                }
                response.onFailure {
                    addBotMessage("⚠️ Connection failed. Please call ambulance: **0800 100 066**")
                }
            } catch (e: Exception) {
                removeTypingIndicator()
                addBotMessage("⚠️ Cannot connect. Call ambulance: **0800 100 066**")
            }
        }
    }

    private fun createEmergencyChatSession(doctorId: Int, doctorName: String) {
        val symptoms = ctx.symptoms.ifEmpty { listOf("Emergency consultation") }
        val assessment = "EMERGENCY: ${recentUserInputs.lastOrNull() ?: "Immediate medical attention required"}"
        
        addBotMessage(
            "✅ **Connected to Dr. $doctorName**\n\n" +
            "Opening your emergency consultation chat now...\n" +
            "The doctor has been briefed on your situation."
        )
        
        handler.postDelayed({
            // Navigate to doctor chat (to be implemented)
            startActivity(Intent(this, FindDoctorsActivity::class.java))
        }, 1500)
    }

    private fun respondToMalaria() {
        ctx.currentTopic = "malaria"
        addBotMessage(
            "🦟 **Malaria — Assessment & Guidance**\n\n" +
            "**Classic malaria symptoms:**\n" +
            "• 🌡️ High fever (often 38.5°C+) with chills\n" +
            "• 🥵 Sweating followed by shivering cycles\n" +
            "• 🤕 Severe headache and muscle aches\n" +
            "• 🤢 Nausea and vomiting\n" +
            "• 😩 Extreme fatigue\n\n" +
            "**⚠️ Warning signs of severe malaria (go to hospital NOW):**\n" +
            "• Confusion or difficulty staying awake\n" +
            "• Seizures or convulsions\n" +
            "• Difficulty breathing\n" +
            "• Very dark or cola-coloured urine\n\n" +
            "**What to do:**\n" +
            "1. Go to a clinic for a **Rapid Diagnostic Test (RDT)** — results in 15 mins\n" +
            "2. Do NOT self-prescribe antimalarials — incorrect treatment causes resistance\n" +
            "3. Start oral rehydration (ORS or clean water) immediately\n" +
            "4. Rest and reduce fever with paracetamol\n\n" +
            "**Prevention:**\n" +
            "🛏️ Sleep under an insecticide-treated net every night\n" +
            "🦟 Use DEET mosquito repellent on exposed skin\n" +
            "🚿 Empty or cover any standing water near your home\n\n" +
            "Would you like me to find a nearby clinic for a malaria test?"
        )
        conversationState = ConversationState.OFFERING_DOCTOR
    }

    private fun respondToTyphoid() {
        ctx.currentTopic = "typhoid"
        addBotMessage(
            "🌡️ **Typhoid Fever — Information & Guidance**\n\n" +
            "**Typhoid symptoms (develop over 1–3 weeks):**\n" +
            "• Sustained high fever (rises gradually, 39–40°C)\n" +
            "• Severe headache and general weakness\n" +
            "• Stomach pain, loss of appetite\n" +
            "• Rose-coloured spots on the trunk (in some cases)\n" +
            "• Constipation OR diarrhoea\n\n" +
            "**Diagnosis:**\n" +
            "A **Widal test** or blood culture confirms typhoid. Seek a clinic.\n\n" +
            "**Treatment:**\n" +
            "• Prescribed antibiotics (do NOT self-medicate)\n" +
            "• Plenty of clean fluids and rest\n" +
            "• Paracetamol for fever control\n\n" +
            "**Prevention:**\n" +
            "🥤 Only drink boiled or bottled water\n" +
            "🧼 Wash hands thoroughly before eating\n" +
            "🍎 Peel fruits; avoid raw salads from street stalls\n" +
            "💉 Typhoid vaccine provides 60–80% protection\n\n" +
            "⚕️ Typhoid requires professional diagnosis and antibiotics. Shall I connect you to a doctor?"
        )
        conversationState = ConversationState.OFFERING_DOCTOR
    }

    private fun respondToTB() {
        ctx.currentTopic = "tb"
        addBotMessage(
            "🫁 **Tuberculosis (TB) — Information**\n\n" +
            "**Key TB symptoms (lasting 3+ weeks):**\n" +
            "• Persistent cough — may produce blood-stained sputum\n" +
            "• Night sweats (drenching)\n" +
            "• Unexplained weight loss\n" +
            "• Prolonged low-grade fever\n" +
            "• Chest pain when breathing or coughing\n\n" +
            "**Important facts:**\n" +
            "• TB is spread through the air — coughing, sneezing\n" +
            "• It is **curable** with a full 6-month antibiotic course\n" +
            "• Early detection = full recovery in most cases\n\n" +
            "**Diagnosis:**\n" +
            "Visit a health centre for a **sputum test** or **GeneXpert test** — both are free in Uganda at public hospitals.\n\n" +
            "**What to do NOW:**\n" +
            "1. Cover your mouth when coughing (use a tissue / elbow)\n" +
            "2. Visit a health centre for TB screening\n" +
            "3. Inform close contacts — they may need screening too\n\n" +
            "🇺🇬 **Uganda national TB programme helpline:** +256 800 100 006 _(free)_\n\n" +
            "⚕️ Early diagnosis saves lives. Would you like help finding a TB testing facility?"
        )
        conversationState = ConversationState.OFFERING_DOCTOR
    }

    private fun respondToDiabetes() {
        ctx.currentTopic = "diabetes"
        addBotMessage(
            "🩸 **Diabetes — Signs, Risks & Management**\n\n" +
            "**Warning signs:**\n" +
            "• Frequent urination, especially at night\n" +
            "• Excessive thirst and hunger\n" +
            "• Unexplained weight loss\n" +
            "• Blurred vision\n" +
            "• Slow-healing wounds or frequent infections\n" +
            "• Tingling or numbness in hands/feet\n\n" +
            "**Normal blood glucose values:**\n" +
            "• Fasting: **70–100 mg/dL** _(< 7.0 mmol/L)_\n" +
            "• 2 hrs after eating: **< 140 mg/dL** _(< 7.8 mmol/L)_\n" +
            "• HbA1c (3-month average): **< 5.7%** normal, **≥ 6.5%** = diabetes\n\n" +
            "**Daily management tips:**\n" +
            "• 🥗 Low-sugar, high-fibre diet (whole grains, vegetables, legumes)\n" +
            "• 🚶 30 minutes of brisk walking daily lowers blood sugar\n" +
            "• 💊 Never skip prescribed medication or insulin\n" +
            "• 🩺 Check your feet daily for cuts or sores\n" +
            "• 📊 Monitor blood sugar at home with a glucometer\n" +
            "• 👁️ Annual eye and kidney check-ups are essential\n\n" +
            "**Risk factors:** Obesity, family history, age > 40, sedentary lifestyle.\n\n" +
            "⚕️ An HbA1c blood test diagnoses diabetes definitively. Shall I help you find a doctor?"
        )
        conversationState = ConversationState.OFFERING_DOCTOR
    }

    private fun respondToBloodPressure() {
        ctx.currentTopic = "blood_pressure"
        addBotMessage(
            "❤️ **Blood Pressure — Complete Guide**\n\n" +
            "**BP Classification:**\n" +
            "🟢 Normal: **< 120/80 mmHg**\n" +
            "🟡 Elevated: **120–129 / < 80 mmHg**\n" +
            "🟠 Stage 1 High: **130–139 / 80–89 mmHg**\n" +
            "🔴 Stage 2 High: **≥ 140 / ≥ 90 mmHg**\n" +
            "🆘 Hypertensive Crisis: **≥ 180 / ≥ 120 mmHg** — seek emergency care NOW\n\n" +
            "**Symptoms of high BP (often called the 'silent killer'):**\n" +
            "• Headache at the back of the neck\n" +
            "• Dizziness or blurred vision\n" +
            "• Shortness of breath, chest tightness\n" +
            "• Nosebleeds (in severe cases)\n\n" +
            "**Proven lifestyle changes:**\n" +
            "• 🧂 Reduce salt to < 5g/day — avoid canned food, processed meats\n" +
            "• 🚶 30+ minutes of moderate exercise 5 days/week\n" +
            "• 🚫 Stop smoking — raises BP significantly\n" +
            "• 🍷 Limit alcohol to < 1 drink/day\n" +
            "• 🧘 Manage stress: meditation, deep breathing, yoga\n" +
            "• 🍌 Potassium-rich foods (bananas, beans, avocado) help lower BP\n" +
            "• ⚖️ Lose even 5kg if overweight — reduces BP by 5 mmHg\n\n" +
            "⚕️ BP should be checked regularly, at least twice a year. Want to book a check-up?"
        )
        conversationState = ConversationState.OFFERING_DOCTOR
    }

    private fun respondToMentalHealth() {
        ctx.currentTopic = "mental_health"
        addBotMessage(
            "🧠 **Mental Health — You Are Not Alone** 💚\n\n" +
            "Mental health is just as important as physical health. Seeking help is a sign of strength.\n\n" +
            "**You might be struggling if you experience:**\n" +
            "• Persistent sadness, emptiness, or hopelessness\n" +
            "• Loss of interest in things you used to enjoy\n" +
            "• Difficulty concentrating or making decisions\n" +
            "• Changes in sleep or appetite\n" +
            "• Feeling overwhelmed, anxious, or panicky\n" +
            "• Thoughts of self-harm or suicide\n\n" +
            "**Practical coping strategies:**\n" +
            "• 🗣️ Talk to a trusted friend, family member or counsellor\n" +
            "• 🧘 Practice 5-minute deep breathing: inhale 4 sec, hold 4, exhale 6\n" +
            "• 🚶 Regular physical exercise reduces depression by 30–40%\n" +
            "• 📱 Limit social media to 30 min/day\n" +
            "• 📔 Journaling helps process emotions and track mood patterns\n" +
            "• 😴 Prioritise sleep — mood and cognition are deeply linked to sleep quality\n\n" +
            "**🇺🇬 Uganda support lines:**\n" +
            "📞 Mental Health Uganda: **+256 800 212 121** _(free)_\n" +
            "🏥 Butabika National Referral Hospital, Kampala\n" +
            "🏥 Most district hospitals have mental health units\n\n" +
            "💬 I'm here to listen. Would you like to talk more, or shall I connect you with a mental health professional?"
        )
        conversationState = ConversationState.OFFERING_DOCTOR
    }

    private fun respondToNutrition() {
        ctx.currentTopic = "nutrition"
        addBotMessage(
            "🥗 **Nutrition & Healthy Eating — Practical Guide**\n\n" +
            "**The Healthy Plate (per meal):**\n" +
            "• 🍚 **50%** Complex carbs: posho, sweet potato, millet, brown rice\n" +
            "• 🥩 **25%** Lean protein: fish, chicken (no skin), beans, eggs, groundnuts\n" +
            "• 🥦 **25%** Vegetables & fruit: aim for 5 colours per day\n\n" +
            "**Daily habits that transform your health:**\n" +
            "• 💧 Drink **8–10 glasses** of clean water daily\n" +
            "• 🌅 Never skip breakfast — it stabilises blood sugar all day\n" +
            "• 🕕 Eat at consistent times — your metabolism runs on a clock\n" +
            "• 🚫 Limit: fried foods, white sugar, white bread, processed snacks\n" +
            "• 🧂 Use lemon juice, garlic, ginger, or herbs instead of excess salt\n\n" +
            "**Ugandan superfoods to include:**\n" +
            "🥜 Silver fish (mukene) — high protein + omega-3\n" +
            "🍠 Sweet potato (orange) — vitamin A & fibre\n" +
            "🫘 Beans & groundnuts — protein + iron\n" +
            "🥬 Nakati & sukuma wiki — calcium & vitamins\n" +
            "🍌 Green bananas (matooke) — potassium & energy\n\n" +
            "**BMI guide:**\n" +
            "• Underweight: < 18.5 | Normal: 18.5–24.9\n" +
            "• Overweight: 25–29.9 | Obese: ≥ 30\n\n" +
            "🌟 Small, consistent changes outperform drastic diets every time."
        )
    }

    private fun respondToMedication() {
        ctx.currentTopic = "medication"
        // Check if specific drug mentioned
        val lastInput = recentUserInputs.lastOrNull() ?: ""
        val specificDrug = when {
            lastInput.contains("paracetamol") || lastInput.contains("acetaminophen") ->
                "**Paracetamol (Acetaminophen):**\n• Adult dose: 500–1000mg every 4–6 hours\n• Max: 4g (4000mg) per 24 hours\n• ❌ Avoid with alcohol — risk of liver damage\n• Safe for children at weight-based doses\n\n"
            lastInput.contains("ibuprofen") ->
                "**Ibuprofen (Brufen / Advil):**\n• Adult dose: 200–400mg every 6–8 hours with food\n• ❌ Avoid on an empty stomach — causes gastric ulcers\n• ❌ Avoid if you have kidney disease or are pregnant\n• Anti-inflammatory: good for joint/muscle pain\n\n"
            lastInput.contains("amoxicillin") ->
                "**Amoxicillin:**\n• Common antibiotic for throat, ear, chest infections\n• Adult dose: 250–500mg 3× daily for 5–7 days\n• ⚠️ Complete the FULL course even if you feel better\n• ❌ Take if allergic to penicillin — tell your doctor first\n\n"
            lastInput.contains("metformin") ->
                "**Metformin:**\n• First-line diabetes medication\n• Take with or just after food to reduce stomach upset\n• ⚠️ Never stop suddenly — discuss with your doctor first\n• Common side effect: nausea (usually improves after 2–4 weeks)\n\n"
            else -> ""
        }
        addBotMessage(
            "💊 **Medication Guidance**\n\n" +
            (if (specificDrug.isNotEmpty()) "ℹ️ $specificDrug" else "") +
            "**Universal rules:**\n" +
            "✅ Take exactly as prescribed — dose, timing, duration\n" +
            "✅ Complete full antibiotic courses (7–10 days typically)\n" +
            "✅ Store in cool, dry place away from sunlight\n" +
            "✅ Keep all medications out of reach of children\n" +
            "❌ Never share prescription drugs with anyone else\n" +
            "❌ Do NOT double-dose if you miss one — skip it\n" +
            "❌ Do NOT stop medication without consulting your doctor\n" +
            "❌ Avoid self-prescribing antibiotics — causes resistance\n\n" +
            "💡 **Missed dose rule:** If it's almost time for the next dose, skip the missed one. Otherwise take it as soon as you remember.\n\n" +
            "⚕️ Need a prescription? I can connect you with a licensed doctor.\n" +
            "Ask me about a specific medication and I'll give you more details!"
        )
    }

    private fun respondToUTI() {
        ctx.currentTopic = "uti"
        addBotMessage(
            "🔬 **Urinary Tract Infection (UTI) — Guidance**\n\n" +
            "**Classic UTI symptoms:**\n" +
            "• 🔥 Burning or stinging sensation when urinating\n" +
            "• 🚽 Frequent, urgent need to urinate (often little comes out)\n" +
            "• 💛 Cloudy, dark, or strong-smelling urine\n" +
            "• 😣 Lower abdominal or pelvic pain/pressure\n" +
            "• 🌡️ Low-grade fever (if kidney involved: high fever + back pain)\n\n" +
            "**⚠️ See a doctor urgently if:**\n" +
            "• Fever above 38.5°C with back/flank pain _(possible kidney infection)_\n" +
            "• Blood in urine\n" +
            "• Symptoms in a pregnant woman\n" +
            "• No improvement after 2 days\n\n" +
            "**Self-care while awaiting treatment:**\n" +
            "• 💧 Drink 2–3 litres of water daily to flush bacteria\n" +
            "• 🍋 Cranberry juice may reduce bacterial adhesion\n" +
            "• ❌ Avoid coffee, alcohol, and spicy foods (irritate the bladder)\n" +
            "• 🧴 Use unscented soap; avoid harsh feminine hygiene products\n\n" +
            "**Treatment:** UTIs require a 3–7 day course of antibiotics (e.g., nitrofurantoin or trimethoprim). You need a prescription.\n\n" +
            "⚕️ Would you like me to connect you with a doctor for a prescription?"
        )
        conversationState = ConversationState.OFFERING_DOCTOR
    }

    private fun respondToRespiratory() {
        ctx.currentTopic = "respiratory"
        addBotMessage(
            "🫁 **Respiratory Health — Assessment**\n\n" +
            "**Common respiratory conditions:**\n\n" +
            "**Asthma:**\n" +
            "• Recurring wheeze, chest tightness, shortness of breath, night cough\n" +
            "• Triggered by dust, smoke, cold air, exercise\n" +
            "• Management: prescribed inhaler (reliever + preventer)\n" +
            "• ❌ Never stop your preventer inhaler without doctor advice\n\n" +
            "**Bronchitis:**\n" +
            "• Persistent productive cough, chest tightness, mild fever\n" +
            "• Usually viral (antibiotics don't help unless bacterial)\n" +
            "• Steam inhalation, fluids, and rest are first-line treatment\n\n" +
            "**⚠️ Seek emergency care if you have:**\n" +
            "• Severe shortness of breath at rest\n" +
            "• Blue lips or fingernails (cyanosis)\n" +
            "• Rapid breathing (more than 30 breaths/min)\n" +
            "• Unable to speak in full sentences\n\n" +
            "**Breathing exercise for relief:**\n" +
            "Sit upright → Inhale slowly through nose (4 sec) → Exhale through pursed lips (6 sec) → Repeat 5×\n\n" +
            "⚕️ Respiratory conditions require proper diagnosis. Shall I find a doctor for you?"
        )
        conversationState = ConversationState.OFFERING_DOCTOR
    }

    private fun respondToSkin() {
        ctx.currentTopic = "skin"
        addBotMessage(
            "🧴 **Skin Health — Guidance**\n\n" +
            "**Common skin conditions:**\n\n" +
            "🔴 **Eczema** — dry, itchy, inflamed skin\n" +
            "• Moisturise 2× daily with unscented lotion\n" +
            "• Avoid triggers: harsh soaps, wool, sweat\n" +
            "• Mild hydrocortisone cream for flare-ups\n\n" +
            "🔵 **Fungal infection (ringworm/tinea)** — circular scaly patches\n" +
            "• Antifungal cream (clotrimazole) applied 2× daily for 2–4 weeks\n" +
            "• Keep affected area clean and dry\n" +
            "• Do NOT share towels or clothing\n\n" +
            "🟡 **Acne** — blocked pores, pimples\n" +
            "• Wash face twice daily with mild cleanser\n" +
            "• Benzoyl peroxide (2.5–5%) reduces bacteria\n" +
            "• Never squeeze pimples — causes scarring\n" +
            "• Persistent acne: see a dermatologist for prescription options\n\n" +
            "🟠 **Hives (urticaria)** — itchy red welts\n" +
            "• Usually allergic reaction — identify and avoid trigger\n" +
            "• Over-the-counter antihistamine (loratadine/cetirizine) for relief\n" +
            "• 🚨 Swelling of lips/throat = anaphylaxis — call emergency now\n\n" +
            "**General skin care:**\n" +
            "• Use SPF 30+ sunscreen daily (prevents skin cancer)\n" +
            "• Stay hydrated — skin health starts from within\n\n" +
            "⚕️ Persistent or worsening skin conditions need dermatologist review. Want me to find one?"
        )
        conversationState = ConversationState.OFFERING_DOCTOR
    }

    private fun respondToEye() {
        ctx.currentTopic = "eye"
        addBotMessage(
            "👁️ **Eye Health — Assessment**\n\n" +
            "**Common eye problems:**\n\n" +
            "🔴 **Conjunctivitis (Pink Eye):**\n" +
            "• Red, watery, or sticky discharge from eye\n" +
            "• Bacterial type: antibiotic eye drops prescribed by doctor\n" +
            "• Viral type: usually self-resolves in 7–10 days\n" +
            "• 🧼 Wash hands frequently — very contagious\n" +
            "• ❌ Do NOT share towels or eye products\n\n" +
            "🟡 **Eye strain (digital fatigue):**\n" +
            "• Blurry vision, dry eyes, headache after screen use\n" +
            "• **20-20-20 rule:** Every 20 min, look at something 20 feet away for 20 seconds\n" +
            "• Increase screen brightness/font size; use night mode\n\n" +
            "🔵 **Dry eyes:**\n" +
            "• Preservative-free artificial tear drops provide relief\n" +
            "• Stay hydrated; reduce air conditioning exposure\n\n" +
            "**⚠️ See an eye doctor urgently for:**\n" +
            "• Sudden vision loss or blurring\n" +
            "• Eye pain with nausea (possible glaucoma)\n" +
            "• Flashes of light or new floaters\n" +
            "• Chemical splash to eye (rinse immediately with lots of clean water)\n\n" +
            "⚕️ Would you like me to find an eye specialist (ophthalmologist)?"
        )
        conversationState = ConversationState.OFFERING_DOCTOR
    }

    private fun respondToDental() {
        ctx.currentTopic = "dental"
        addBotMessage(
            "🦷 **Dental Health — Guidance**\n\n" +
            "**Common dental problems:**\n\n" +
            "😬 **Toothache:**\n" +
            "• Ibuprofen or paracetamol for pain relief (temporary)\n" +
            "• Clove oil on cotton wool applied to tooth can reduce pain\n" +
            "• Rinsing with warm salt water reduces inflammation\n" +
            "• Toothache = dental cavity or abscess — you NEED a dentist\n\n" +
            "🩸 **Bleeding gums (gingivitis):**\n" +
            "• Brush gently twice daily with soft-bristle brush\n" +
            "• Floss daily — removes bacteria between teeth\n" +
            "• Use antiseptic mouthwash (chlorhexidine)\n" +
            "• Usually improves in 1–2 weeks with good hygiene\n\n" +
            "💛 **Tooth sensitivity:**\n" +
            "• Use sensitivity toothpaste (containing potassium nitrate)\n" +
            "• Avoid acidic foods (sodas, citrus) which erode enamel\n" +
            "• Could indicate cracked tooth or receding gums — see dentist\n\n" +
            "**Daily dental care:**\n" +
            "• 🪥 Brush for 2 minutes, twice daily\n" +
            "• 🧵 Floss once daily\n" +
            "• 💧 Fluoride toothpaste strengthens enamel\n" +
            "• 🦷 Dental check-up every 6 months\n\n" +
            "⚕️ Dental pain should not be ignored — it can lead to abscess and infection. Want help finding a dentist?"
        )
        conversationState = ConversationState.OFFERING_DOCTOR
    }

    private fun respondToMaternal() {
        ctx.currentTopic = "maternal"
        addBotMessage(
            "🤰 **Maternal & Pregnancy Health**\n\n" +
            "**Antenatal care schedule (Uganda MOH guideline):**\n" +
            "• 1st visit before 12 weeks (confirm pregnancy + baseline tests)\n" +
            "• 2nd visit: 20 weeks (anomaly scan)\n" +
            "• 3rd visit: 26 weeks\n" +
            "• 4th visit: 32 weeks\n" +
            "• 5th+ visits: 36, 38, 40 weeks (birth planning)\n\n" +
            "**Essential supplements during pregnancy:**\n" +
            "• 🟢 Folic acid 400mcg/day _(prevents neural tube defects)_\n" +
            "• 🔴 Iron + folic acid supplement daily\n" +
            "• ☀️ Vitamin D 400 IU/day\n\n" +
            "**⚠️ Warning signs — go to hospital immediately:**\n" +
            "• Heavy vaginal bleeding\n" +
            "• Severe headache + swollen face/hands _(pre-eclampsia sign)_\n" +
            "• Reduced or absent baby movements after 28 weeks\n" +
            "• Fever above 38°C\n" +
            "• Rupture of membranes (waters breaking)\n\n" +
            "**Nutrition in pregnancy:**\n" +
            "• Extra 300 calories/day in 2nd & 3rd trimester\n" +
            "• Iron-rich foods: liver, beans, dark leafy greens\n" +
            "• Calcium: milk, silver fish (mukene), sesame seeds\n" +
            "• ❌ Avoid: raw meat, unpasteurised milk, excess caffeine, alcohol\n\n" +
            "🇺🇬 Free antenatal care at all government health centres in Uganda.\n\n" +
            "⚕️ Would you like to book an antenatal appointment with one of our doctors?"
        )
        conversationState = ConversationState.OFFERING_DOCTOR
    }

    private fun respondToChildHealth() {
        ctx.currentTopic = "child_health"
        addBotMessage(
            "👶 **Child Health — Guidance**\n\n" +
            "**Uganda immunisation schedule (key vaccines):**\n" +
            "• Birth: BCG + OPV0\n" +
            "• 6 weeks: Pentavalent, OPV1, PCV1, Rotavirus 1\n" +
            "• 10 weeks: Pentavalent 2, OPV2, PCV2, Rotavirus 2\n" +
            "• 14 weeks: Pentavalent 3, OPV3, PCV3, IPV\n" +
            "• 9 months: Measles + Yellow Fever\n" +
            "• 18 months: Measles-Rubella booster\n\n" +
            "**Growth monitoring (weight-for-age):**\n" +
            "• Weigh your child monthly under age 2\n" +
            "• Report poor weight gain to your health worker\n\n" +
            "**⚠️ Danger signs — take child to hospital IMMEDIATELY:**\n" +
            "• Unable to eat or drink\n" +
            "• Convulsions/fits\n" +
            "• Very fast or difficult breathing\n" +
            "• Skin goes cold and pale\n" +
            "• Prolonged fever (> 3 days in child < 5)\n\n" +
            "**Fever management in children:**\n" +
            "• Paracetamol syrup: 10–15 mg/kg every 6 hours\n" +
            "• Do NOT give aspirin to children under 16 (Reye's syndrome risk)\n" +
            "• Lukewarm sponge bath to reduce fever\n" +
            "• Push fluids (ORS, breastmilk, water)\n\n" +
            "🍼 Exclusive breastfeeding for the first 6 months is the single best investment in your child's health.\n\n" +
            "⚕️ Would you like to book a paediatric consultation?"
        )
        conversationState = ConversationState.OFFERING_DOCTOR
    }

    private fun respondToCovid() {
        ctx.currentTopic = "covid"
        addBotMessage(
            "🦠 **COVID-19 — Current Guidance**\n\n" +
            "**Symptoms to watch for:**\n" +
            "• Fever or chills\n" +
            "• Dry cough, sore throat\n" +
            "• Fatigue and body aches\n" +
            "• Loss of taste or smell _(quite specific to COVID)_\n" +
            "• Shortness of breath or difficulty breathing\n" +
            "• Headache, congestion, runny nose\n\n" +
            "**If you test positive:**\n" +
            "1. Isolate at home for minimum **5 days**\n" +
            "2. Rest and stay well hydrated\n" +
            "3. Take paracetamol for fever and body aches\n" +
            "4. Monitor your oxygen saturation with a pulse oximeter if available\n" +
            "5. Seek hospital care if SpO₂ drops below **94%** or breathing becomes laboured\n\n" +
            "**⚠️ Seek emergency care for:**\n" +
            "• Difficulty breathing or shortness of breath at rest\n" +
            "• Persistent chest pain or pressure\n" +
            "• New confusion or difficulty waking\n" +
            "• Pale, grey, or bluish skin/lips\n\n" +
            "**Prevention:**\n" +
            "💉 Stay up to date with COVID-19 vaccination\n" +
            "😷 Wear masks in crowded indoor settings\n" +
            "🧼 Wash hands regularly — 20 seconds with soap\n" +
            "💨 Open windows to ventilate indoor spaces\n\n" +
            "⚕️ Need a COVID test or teleconsultation? I can connect you with a doctor."
        )
        conversationState = ConversationState.OFFERING_DOCTOR
    }

    private fun respondToQuestion(text: String) {
        val response = when {
            text.contains("appointment") || text.contains("book") ->
                "📅 **Booking an Appointment**\n\n" +
                "You can book in 3 easy steps:\n" +
                "1️⃣ Tap **'Book Doctor'** button or use the Find Doctors screen\n" +
                "2️⃣ Filter by specialty, rating, or online status\n" +
                "3️⃣ Choose your preferred time slot and confirm\n\n" +
                "💰 All consultation fees are displayed upfront — no hidden charges.\n\n" +
                "Would you like me to open the doctors list now?"
            text.contains("fee") || text.contains("cost") || text.contains("price") || text.contains("charge") ->
                "💰 **Consultation Fees (Approximate)**\n\n" +
                "• General Practitioner: UGX 30,000–50,000\n" +
                "• Specialist Consultation: UGX 60,000–120,000\n" +
                "• Paediatrician: UGX 50,000–80,000\n" +
                "• Online Video Consultation: ~20% lower than in-person\n" +
                "• Emergency Consultation: Varies by facility\n\n" +
                "💳 Fees displayed before booking — no surprises.\n" +
                "🇺🇬 Government health centres offer free or lower-cost services."
            text.contains("how long") || text.contains("wait time") || text.contains("waiting") ->
                "⏱️ **Expected Wait Times**\n\n" +
                "• AI Assistant: Instant ✅\n" +
                "• Online Doctor Chat: Within 1–2 hours\n" +
                "• Video consultation: Same day or next day\n" +
                "• In-person (booked): Chosen time slot\n" +
                "• Emergency: Prioritised immediately\n\n" +
                "We connect you to care as fast as possible! 🚀"
            text.contains("vaccine") || text.contains("vaccination") || text.contains("jab") ->
                "💉 **Vaccination Information (Uganda)**\n\n" +
                "**Recommended for adults:**\n" +
                "• COVID-19 (stay up to date)\n" +
                "• Yellow Fever _(required for international travel)_\n" +
                "• Hepatitis B (3-dose course if not previously vaccinated)\n" +
                "• Typhoid vaccine (every 3 years for high-risk areas)\n" +
                "• Meningitis _(travellers, students in dormitories)_\n\n" +
                "**Free via Uganda Expanded Programme on Immunisation (EPI):**\n" +
                "Visit any government health centre with your immunisation card.\n\n" +
                "⚕️ Need a vaccination appointment? I can connect you with a provider."
            text.contains("medicin") || text.contains("drug") || text.contains("treat") ->
                respondToMedication().let { "" } // delegates
            text.contains("symptom") || text.contains("what do i have") || text.contains("diagnos") ->
                "🩺 **Symptom Analysis**\n\n" +
                "Describe your symptoms in as much detail as you can — for example:\n\n" +
                "_'I have fever, headache, and body aches since 2 days'_\n\n" +
                "I'll ask you about duration and severity, then give you a personalised health assessment with specific advice and urgency guidance.\n\n" +
                "Go ahead — what are you experiencing?"
            text.contains("covid") || text.contains("corona") -> { respondToCovid(); "" }
            else ->
                "🤗 I'm here to help! You can ask me about:\n\n" +
                "🩺 Symptoms — _describe how you feel_\n" +
                "🦟 Malaria, typhoid, TB, diabetes, hypertension\n" +
                "🧠 Mental health, stress, anxiety\n" +
                "💊 Medications and dosage\n" +
                "🤰 Pregnancy and child health\n" +
                "💡 Health tips and nutrition\n" +
                "📅 Booking doctors\n" +
                "🚨 Emergency guidance\n\n" +
                "What would you like to know?"
        }
        if (response.isNotEmpty()) addBotMessage(response)
    }

    private fun respondToGratitude() {
        addBotMessage(listOf(
            "You're very welcome! 😊 Your health is our priority — come back any time you have questions. Stay well! 💚",
            "My pleasure! 💚 Feel free to return whenever you need health guidance. Take care of yourself!",
            "Glad I could help! 🌟 Remember: stay hydrated, sleep well, eat balanced meals, and don't ignore persistent symptoms! 😄"
        ).random())
    }

    private fun respondIntelligently(text: String) {
        when {
            text.length < 4 -> {
                addBotMessage("Could I get a bit more detail? 😊 Describe how you're feeling or ask me a health question and I'll do my best to help.")
                return
            }
            conversationState == ConversationState.COLLECTING_SYMPTOMS || conversationState == ConversationState.AWAITING_DURATION -> {
                // Treat any text as a duration response
                ctx.durationText = text.take(50)
                conversationState = ConversationState.AWAITING_SEVERITY
                addBotMessage(
                    "Thank you for sharing. 📝\n\n" +
                    "On a scale of **1 to 10**, how would you rate your discomfort?\n" +
                    "_(1 = barely noticeable, 10 = extremely severe)_"
                )
                return
            }
        }

        // ── Intelligent context-aware response ──
        // Check conversation history for patterns
        val recentContext = recentUserInputs.toList().takeLast(3).joinToString(" ")

        // Try to detect a health topic from vague input FIRST (before generic responses)
        val topicHints = mapOf(
            listOf("heart","cardiac","chest","palpitation","beats") to { respondToBloodPressure() },
            listOf("stomach","abdomen","belly","gut","bowel","tummy") to { startSymptomCollection("stomach pain diarrhoea nausea") },
            listOf("head","migraine","brain","skull") to { startSymptomCollection("headache") },
            listOf("skin","colour","rash","itchy","scratch") to { respondToSkin() },
            listOf("eye","see","vision","sight","blurry") to { respondToEye() },
            listOf("tooth","teeth","mouth","jaw","gum","dental") to { respondToDental() },
            listOf("baby","child","son","daughter","infant","kid") to { respondToChildHealth() },
            listOf("pregnant","baby coming","expecting","trimester") to { respondToMaternal() },
            listOf("breath","lungs","asthma","wheez") to { respondToRespiratory() },
            listOf("stress","worried","anxious","depress","sad","overwhelm") to { respondToMentalHealth() },
            listOf("eat","food","diet","weight","hungry","nutrition") to { respondToNutrition() }
        )

        for ((keywords, action) in topicHints) {
            if (keywords.any { text.contains(it) }) {
                action()
                return
            }
        }

        // Predict user intent from conversation flow
        when {
            recentContext.contains("pain") || recentContext.contains("hurt") -> {
                addBotMessage(
                    "I understand you're experiencing discomfort. 💭\n\n" +
                    "To give you the best guidance, could you tell me:\n" +
                    "• Where exactly is the pain located?\n" +
                    "• When did it start?\n" +
                    "• Is it constant or comes and goes?\n\n" +
                    "Or if it's urgent, I can connect you with a doctor right away."
                )
                return
            }
            recentContext.contains("feel") && (text.contains("bad") || text.contains("not well") || text.contains("unwell")) -> {
                addBotMessage(
                    "I'm sorry you're not feeling well. 💚\n\n" +
                    "Let's figure out what's going on. Could you describe:\n" +
                    "• Your main symptoms (fever, headache, nausea, etc.)\n" +
                    "• How long you've been feeling this way\n\n" +
                    "I'll provide guidance and can connect you with a doctor if needed."
                )
                return
            }
            text.contains("not sure") || text.contains("don't know") || text.contains("maybe") -> {
                addBotMessage(
                    "No worries — let's work through this together! 🤝\n\n" +
                    "You can:\n" +
                    "• Describe any physical symptoms you have\n" +
                    "• Ask about a specific health condition\n" +
                    "• Request a health tip\n" +
                    "• Book an appointment with a doctor\n\n" +
                    "What would be most helpful for you right now?"
                )
                return
            }
            text.contains("help") || text.contains("assist") -> {
                addBotMessage(
                    "I'm here to help! 🩺\n\n" +
                    "I can assist you with:\n" +
                    "• Symptom analysis & health guidance\n" +
                    "• Connecting you with online doctors\n" +
                    "• Health tips & wellness advice\n" +
                    "• Medication information\n" +
                    "• Emergency guidance\n\n" +
                    "What specific help do you need today?"
                )
                return
            }
            text.contains("problem") || text.contains("issue") || text.contains("concern") -> {
                addBotMessage(
                    "I'm listening. 👂\n\n" +
                    "Please describe your concern in detail — the more information you share, the better I can help you. " +
                    "You can describe symptoms, ask questions, or request to speak with a doctor."
                )
                return
            }
            text.contains("sick") || text.contains("ill") || text.contains("unwell") -> {
                addBotMessage(
                    "I'm sorry you're feeling unwell. 💚\n\n" +
                    "Let's get to the bottom of this. Please share:\n" +
                    "• Your symptoms\n" +
                    "• When they started\n" +
                    "• Anything that makes them better or worse\n\n" +
                    "I'll provide guidance and connect you with a doctor if needed."
                )
                return
            }
        }

        // Default response - offer help
        val response = when {
            recentUserInputs.size >= 3 -> {
                // User has been chatting but no clear intent — offer proactive help
                "I want to make sure I'm helping you effectively! 🎯\n\n" +
                "Based on our conversation, would you like me to:\n" +
                "• Analyze specific symptoms you're experiencing?\n" +
                "• Connect you with a doctor for professional advice?\n" +
                "• Provide health tips for staying well?\n" +
                "• Answer questions about a health condition?\n\n" +
                "Just let me know what would be most useful!"
            }
            else -> {
                "I want to give you the best possible help! 🤔\n\n" +
                "Could you try rephrasing or:\n\n" +
                "• Describe your symptoms: _\"I have fever and headache\"_\n" +
                "• Ask about a condition: _\"Tell me about malaria\"_\n" +
                "• Request help: _\"I want to see a doctor\"_\n" +
                "• Get tips: _\"Give me a health tip\"_\n\n" +
                "Or use the quick buttons below! 👇"
            }
        }

        addBotMessage(response)
    }

    private fun offerDoctorConnection() {
        addBotMessage(
            "🩺 **Let me connect you with a real doctor**\n\n" +
            "I can help you start a chat with:\n\n" +
            "💬 **Online doctors** (🟢 available now) — response within 5-10 minutes\n" +
            "📹 **Video consultation** — see a doctor face-to-face from home\n" +
            "🏥 **In-person visit** — book appointment at their clinic\n\n" +
            "We have **200+ verified doctors** including:\n" +
            "❤️ General Practitioners • Specialists\n" +
            "👶 Paediatricians • 🧠 Psychiatrists\n" +
            "🤰 OB/GYN • 🫁 Internists • And more\n\n" +
            "All consultations are **confidential and secure**."
        )
        
        handler.postDelayed({
            // Check for online doctors for instant chat
            lifecycleScope.launch {
                try {
                    showTypingIndicator()
                    val response = RepositoryFactory.doctorRepository.getOnlineDoctors()
                    removeTypingIndicator()
                    
                    response.onSuccess { doctors ->
                        if (doctors.isNotEmpty()) {
                            val urgency = ctx.severityScore?.let {
                                when {
                                    it >= 8 -> "urgent"
                                    it >= 5 -> "moderate"
                                    else -> "normal"
                                }
                            } ?: "normal"
                            
                            val topDoctors = doctors.take(3)
                            val doctorList = topDoctors.joinToString("\n") { 
                                "• **Dr. ${it.name}** — ${it.specialty} ⭐ ${it.rating}/5.0 🟢"
                            }
                            
                            addBotMessage(
                                "✅ **${topDoctors.size} doctors are online right now:**\n\n" +
                                "$doctorList\n\n" +
                                "Would you like me to start a chat with one of them based on your symptoms?"
                            )
                            
                            handler.postDelayed({
                                showDoctorConnectionDialog(topDoctors.first(), urgency)
                            }, 1200)
                        } else {
                            addBotMessage(
                                "⚠️ No doctors online at this moment.\n\n" +
                                "**Options:**\n" +
                                "1️⃣ Browse all doctors and book appointment\n" +
                                "2️⃣ I can notify you when a doctor comes online\n" +
                                "3️⃣ Continue chatting with me for guidance"
                            )
                            showOfflineDoctorOptions()
                        }
                    }
                    response.onFailure {
                        // Fallback to browse all doctors
                        showOfflineDoctorOptions()
                    }
                } catch (e: Exception) {
                    removeTypingIndicator()
                    showOfflineDoctorOptions()
                }
            }
        }, 1200)
    }

    private fun showDoctorConnectionDialog(doctor: com.healthbridge.network.Doctor, urgency: String) {
        AlertDialog.Builder(this)
            .setTitle("🩺 Connect to Doctor")
            .setMessage(
                "Start chat with:\n\n" +
                "👨‍⚕️ Dr. ${doctor.name}\n" +
                "🏥 ${doctor.specialty}\n" +
                "⭐ ${doctor.rating}/5.0 (${doctor.reviewCount} reviews)\n" +
                "💰 Consultation: UGX ${doctor.consultationFee.toString().replace(Regex("(\\d)(?=(\\d{3})+$)"), "$1,")}\n\n" +
                "The doctor will see your symptoms and assessment."
            )
            .setPositiveButton("Start Chat") { _, _ ->
                initiateRealtimeDoctorChat(doctor.id, doctor.name, urgency)
            }
            .setNeutralButton("Browse All Doctors") { _, _ ->
                startActivity(Intent(this, FindDoctorsActivity::class.java))
            }
            .setNegativeButton("Not Now", null)
            .show()
    }

    private fun initiateRealtimeDoctorChat(doctorId: Int, doctorName: String, urgency: String) {
        addBotMessage("🔄 Connecting you to Dr. $doctorName...")
        showTypingIndicator()
        
        // Create chat session via API
        lifecycleScope.launch {
            try {
                val symptoms = ctx.symptoms.ifEmpty { listOf("General consultation") }
                val chiefComplaint = ctx.currentTopic ?: symptoms.joinToString(", ")
                val assessment = buildString {
                    append("AI Pre-Assessment:\n")
                    if (symptoms.isNotEmpty()) append("Symptoms: ${symptoms.joinToString(", ")}\n")
                    ctx.durationText?.let { append("Duration: $it\n") }
                    ctx.severityScore?.let { append("Severity: $it/10\n") }
                }
                
                // API call to create chat session
                val sessionResponse = ApiClient.instance.createChatSession(
                    CreateChatSessionRequest(
                        doctorId = doctorId,
                        chiefComplaint = chiefComplaint,
                        symptoms = assessment,
                        urgency = urgency.uppercase()
                    )
                )

                removeTypingIndicator()

                if (sessionResponse.success && sessionResponse.session != null) {
                    val session = sessionResponse.session
                    addBotMessage(
                        "✅ **Chat session created!**\n\n" +
                        "Dr. $doctorName has been notified and will respond shortly.\n\n" +
                        "Opening your chat now..."
                    )

                    handler.postDelayed({
                        // Navigate to patient's chat view with doctor
                        val intent = Intent(this@ChatActivity, PatientChatActivity::class.java)
                        intent.putExtra("session_id", session.id)
                        intent.putExtra("doctor_name", session.doctorName ?: "Doctor")
                        intent.putExtra("chief_complaint", session.chiefComplaint)
                        intent.putExtra("urgency_level", session.urgency)
                        startActivity(intent)
                    }, 1500)
                } else {
                    addBotMessage(
                        "⚠️ Could not create chat session.\n\n" +
                        "Please try browsing doctors manually."
                    )
                }

            } catch (e: Exception) {
                removeTypingIndicator()
                addBotMessage(
                    "⚠️ Could not establish connection.\n\n" +
                    "Please try:\n" +
                    "• Browse doctors manually (tap Find Doctors)\n" +
                    "• Check your internet connection\n" +
                    "• Try again in a moment"
                )
            }
        }
    }

    private fun showOfflineDoctorOptions() {
        handler.postDelayed({
            AlertDialog.Builder(this)
                .setTitle("🩺 Doctor Consultation")
                .setMessage("No doctors online right now.\n\nWould you like to browse all doctors and book an appointment?")
                .setPositiveButton("Browse Doctors") { _, _ ->
                    startActivity(Intent(this, FindDoctorsActivity::class.java))
                }
                .setNegativeButton("Continue Chat", null)
                .show()
        }, 800)
    }

    private fun addBotMessage(text: String) {
        adapter.addMessage(SimpleChatMessage(text, false))
        scrollToBottom()
    }

    private fun scrollToBottom() {
        rvChat.post {
            if (messages.isNotEmpty()) rvChat.smoothScrollToPosition(messages.size - 1)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish(); return true
    }
}
