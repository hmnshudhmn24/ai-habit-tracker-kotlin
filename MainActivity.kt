package com.example.habittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.*
import com.google.mlkit.nl.smartreply.SmartReply
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.LocalDate

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val date: String
)

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY date DESC")
    fun getAll(): Flow<List<Habit>>

    @Insert
    suspend fun insert(habit: Habit)
}

@Database(entities = [Habit::class], version = 1)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
}

class HabitViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        HabitDatabase::class.java,
        "habit-db"
    ).build()

    val habits: Flow<List<Habit>> = db.habitDao().getAll()

    suspend fun addHabit(name: String) {
        db.habitDao().insert(Habit(name = name, date = LocalDate.now().toString()))
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                HabitTrackerApp()
            }
        }
    }
}

@Composable
fun HabitTrackerApp(habitViewModel: HabitViewModel = viewModel()) {
    val coroutineScope = rememberCoroutineScope()
    var habitName by remember { mutableStateOf("") }
    val habits by habitViewModel.habits.collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = habitName,
            onValueChange = { habitName = it },
            label = { Text("Enter Habit") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            coroutineScope.launch {
                habitViewModel.addHabit(habitName)
                habitName = ""
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Add Habit")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Your Habits", style = MaterialTheme.typography.titleLarge)
        LazyColumn(modifier = Modifier.fillMaxHeight().padding(top = 8.dp)) {
            items(habits) { habit ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(habit.name, style = MaterialTheme.typography.titleMedium)
                        Text("Date: ${habit.date}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

// Dummy AI Smart Suggestion (using Google ML Kit's SmartReply)
fun generateSmartSuggestions(userMessage: String, onResult: (List<String>) -> Unit) {
    val conversation = mutableListOf<com.google.mlkit.nl.smartreply.TextMessage>()
    conversation.add(com.google.mlkit.nl.smartreply.TextMessage.createForLocalUser(userMessage, System.currentTimeMillis()))
    SmartReply.getClient()
        .suggestReplies(conversation)
        .addOnSuccessListener { result ->
            if (result.status == SmartReply.SUGGESTION_SUCCESS) {
                val suggestions = result.suggestions.map { it.text }
                onResult(suggestions)
            }
        }
}
