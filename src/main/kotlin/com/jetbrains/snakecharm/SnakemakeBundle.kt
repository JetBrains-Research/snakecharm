package com.jetbrains.snakecharm

import com.intellij.AbstractBundle
import com.intellij.reference.SoftReference
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.lang.ref.Reference
import java.util.*

class SnakemakeBundle {
    companion object {
        private var ourBundle: Reference<ResourceBundle>? = null
        @NonNls
        private const val BUNDLE = "SnakemakeBundle"

        // Cached loading
        private val bundle: ResourceBundle?
            get() {
                var bundle = SoftReference.dereference(ourBundle)
                if (bundle == null) {
                    bundle = ResourceBundle.getBundle(BUNDLE)
                    ourBundle = SoftReference(bundle)
                }
                return bundle
            }

        fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
            return AbstractBundle.message(bundle!!, key, *params)
        }
    }
}