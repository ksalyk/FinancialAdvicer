package kz.fearsom.financiallifev2.data

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.value
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
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

@OptIn(ExperimentalForeignApi::class)
actual class SecureStorage {

    private fun baseQuery(key: String) = NSMutableDictionary().also { d ->
        d.setObject(kSecClassGenericPassword!!, kSecClass!!)
        d.setObject(SERVICE_NAME, kSecAttrService!!)
        d.setObject(key, kSecAttrAccount!!)
    }

    actual fun save(key: String, value: String) {
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        // Always delete first so we don't get errSecDuplicateItem
        SecItemDelete(baseQuery(key))
        val addQuery = baseQuery(key)
        addQuery.setObject(data, kSecValueData!!)
        addQuery.setObject(kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly!!, kSecAttrAccessible!!)
        SecItemAdd(addQuery, null)
    }

    actual fun get(key: String): String? = memScoped {
        val query = baseQuery(key)
        query.setObject(true, kSecReturnData!!)
        query.setObject(kSecMatchLimitOne!!, kSecMatchLimit!!)
        val resultRef = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, resultRef.ptr)
        if (status != errSecSuccess) return null
        val data = resultRef.value as? NSData ?: return null
        NSString.create(data, NSUTF8StringEncoding) as? String
    }

    actual fun clear(key: String) {
        SecItemDelete(baseQuery(key))
    }

    companion object {
        private const val SERVICE_NAME = "kz.fearsom.financiallifev2"
    }
}
