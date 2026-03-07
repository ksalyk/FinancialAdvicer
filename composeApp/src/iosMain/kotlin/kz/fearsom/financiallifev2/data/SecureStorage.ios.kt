package kz.fearsom.financiallifev2.data

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class SecureStorage {

    private fun baseQuery(key: String): CFDictionaryRef? {
        val dict = CFDictionaryCreateMutable(null, 3, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
        CFDictionarySetValue(dict, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(dict, kSecAttrService, CFBridgingRetain(NSString.create(string = SERVICE_NAME)))
        CFDictionarySetValue(dict, kSecAttrAccount, CFBridgingRetain(NSString.create(string = key)))
        return dict
    }

    actual fun save(key: String, value: String) {
        val data = NSString.create(string = value).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        // Always delete first so we don't get errSecDuplicateItem
        baseQuery(key)?.let { SecItemDelete(it) }

        val addDict = CFDictionaryCreateMutable(null, 5, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
        CFDictionarySetValue(addDict, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(addDict, kSecAttrService, CFBridgingRetain(NSString.create(string = SERVICE_NAME)))
        CFDictionarySetValue(addDict, kSecAttrAccount, CFBridgingRetain(NSString.create(string = key)))
        CFDictionarySetValue(addDict, kSecValueData, CFBridgingRetain(data))
        CFDictionarySetValue(addDict, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)

        SecItemAdd(addDict, null)
    }

    actual fun get(key: String): String? = memScoped {
        val dict = CFDictionaryCreateMutable(null, 5, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
        CFDictionarySetValue(dict, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(dict, kSecAttrService, CFBridgingRetain(NSString.create(string = SERVICE_NAME)))
        CFDictionarySetValue(dict, kSecAttrAccount, CFBridgingRetain(NSString.create(string = key)))
        CFDictionarySetValue(dict, kSecReturnData, kCFBooleanTrue)
        CFDictionarySetValue(dict, kSecMatchLimit, kSecMatchLimitOne)

        val resultRef = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(dict, resultRef.ptr)
        if (status != errSecSuccess) return null

        val data = CFBridgingRelease(resultRef.value) as? NSData ?: return null
        return NSString.create(data, NSUTF8StringEncoding) as? String
    }

    actual fun clear(key: String) {
        baseQuery(key)?.let { SecItemDelete(it) }
    }

    companion object {
        private const val SERVICE_NAME = "kz.fearsom.financiallifev2"
    }
}
