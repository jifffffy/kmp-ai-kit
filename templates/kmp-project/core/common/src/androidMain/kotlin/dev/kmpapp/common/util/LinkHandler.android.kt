package dev.kmpapp.common.util

import android.content.Context
import android.content.Intent
import android.net.Uri

class AndroidLinkHandler(
    private val context: Context,
) : LinkHandler {
    override fun openLink(link: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
