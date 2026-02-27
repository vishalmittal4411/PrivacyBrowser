package com.privacybrowser

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class BrowserProfile(
    val id: String,
    val name: String,
    val icon: Int,
    val color: String
)

class MainActivity : AppCompatActivity() {

    private val profiles = mutableListOf<BrowserProfile>()
    private lateinit var adapter: ProfileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load saved profiles
        loadProfiles()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        adapter = ProfileAdapter(profiles) { profile ->
            openBrowser(profile)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.btnAddProfile).setOnClickListener {
            showAddProfileDialog()
        }
    }

    private fun loadProfiles() {
        val prefs = getSharedPreferences("profiles", MODE_PRIVATE)
        val count = prefs.getInt("count", 0)
        for (i in 0 until count) {
            val id = prefs.getString("id_$i", null) ?: continue
            val name = prefs.getString("name_$i", "Profile $i") ?: "Profile $i"
            val color = prefs.getString("color_$i", "#FF5722") ?: "#FF5722"
            profiles.add(BrowserProfile(id, name, android.R.drawable.ic_menu_myplaces, color))
        }
        if (profiles.isEmpty()) {
            // Add default profile
            addProfile("Main Profile", "#2196F3")
        }
    }

    private fun saveProfiles() {
        val prefs = getSharedPreferences("profiles", MODE_PRIVATE).edit()
        prefs.putInt("count", profiles.size)
        profiles.forEachIndexed { i, p ->
            prefs.putString("id_$i", p.id)
            prefs.putString("name_$i", p.name)
            prefs.putString("color_$i", p.color)
        }
        prefs.apply()
    }

    private fun addProfile(name: String, color: String) {
        val id = "profile_${System.currentTimeMillis()}"
        profiles.add(BrowserProfile(id, name, android.R.drawable.ic_menu_myplaces, color))
        saveProfiles()
        adapter.notifyDataSetChanged()
    }

    private fun showAddProfileDialog() {
        val input = EditText(this)
        input.hint = "Profile name (e.g., Work, Personal)"
        input.setPadding(48, 24, 48, 24)

        val colors = arrayOf("#2196F3", "#E91E63", "#4CAF50", "#FF9800", "#9C27B0", "#F44336")
        val colorNames = arrayOf("Blue", "Pink", "Green", "Orange", "Purple", "Red")

        var selectedColor = colors[0]
        val colorSpinner = Spinner(this)
        val colorAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, colorNames)
        colorSpinner.adapter = colorAdapter
        colorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                selectedColor = colors[pos]
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
            addView(input)
            addView(colorSpinner)
        }

        AlertDialog.Builder(this)
            .setTitle("New Private Profile")
            .setView(layout)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().ifBlank { "Profile ${profiles.size + 1}" }
                addProfile(name, selectedColor)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openBrowser(profile: BrowserProfile) {
        val intent = Intent(this, BrowserActivity::class.java).apply {
            putExtra("PROFILE_ID", profile.id)
            putExtra("PROFILE_NAME", profile.name)
            putExtra("PROFILE_COLOR", profile.color)
        }
        startActivity(intent)
    }

    inner class ProfileAdapter(
        private val items: List<BrowserProfile>,
        private val onClick: (BrowserProfile) -> Unit
    ) : RecyclerView.Adapter<ProfileAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tvProfileName)
            val info: TextView = view.findViewById(R.id.tvProfileInfo)
            val icon: ImageView = view.findViewById(R.id.ivProfileIcon)
            val card: View = view.findViewById(R.id.cardProfile)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_profile, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val profile = items[position]
            holder.name.text = profile.name
            holder.info.text = "🔒 Isolated • Anti-tracking • Private"
            holder.card.setOnClickListener { onClick(profile) }
        }

        override fun getItemCount() = items.size
    }
}
