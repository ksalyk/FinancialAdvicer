package kz.fearsom.financiallifev2.di

import kz.fearsom.financiallifev2.data.SecureStorage
import org.koin.dsl.module

val iosModule = module {
    single { SecureStorage() }
}
