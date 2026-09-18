package dev.kmpapp.common.util

import java.awt.Desktop
import java.net.URI

class DesktopLinkHandler : LinkHandler {
    override fun openLink(link: String) {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(link))
        }
    }
}
