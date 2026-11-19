package app.dabbulu

import android.app.Application
import android.content.Context
import app.dabbulu.data.AppDatabase
import app.dabbulu.data.ExpenseRepository
import app.dabbulu.data.SettingsRepository

class DabbuluApp : Application() {
    val repository: ExpenseRepository by lazy {
        ExpenseRepository(AppDatabase.build(this), SettingsRepository(this))
    }
}

val Context.repository: ExpenseRepository get() = (applicationContext as DabbuluApp).repository
