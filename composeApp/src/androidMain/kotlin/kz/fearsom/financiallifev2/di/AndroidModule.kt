package kz.fearsom.financiallifev2.di

import kz.fearsom.financiallifev2.data.SecureStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { SecureStorage(androidContext()) }
}
