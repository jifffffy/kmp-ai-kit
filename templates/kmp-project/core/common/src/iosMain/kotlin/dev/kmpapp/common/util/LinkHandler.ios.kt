package dev.kmpapp.common.util

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

class IOSLinkHandler : LinkHandler {
    override fun openLink(link: String) {
        val nsUrl = NSURL.URLWithString(link)
        if (nsUrl != null && UIApplication.sharedApplication.canOpenURL(nsUrl)) {
            UIApplication.sharedApplication.openURL(nsUrl)
        }
    }
}
