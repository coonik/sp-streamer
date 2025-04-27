package com.streamerbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.res.Resources
import android.graphics.Rect

class MainService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        startChecking()
    }

    private var startButtonPosition: Pair<Float, Float>? = null

    private fun startChecking() {
        Thread {
            while (true) {
                val root = rootInActiveWindow ?: continue

                val popup = findPartialText(root, "Vòng quay")
                if (popup != null) {
                    if (startButtonPosition != null) {
                        if (startButtonPosition != null) {
                            val position = startButtonPosition
                            spamClickAtPosition(position!!.first, position.second)
                            Thread.sleep(10) // Sleep ngắn
                            continue
                        }
                    } else {
                        val startButton = findPartialText(root, "Bắt đầu trong")
                        if (startButton != null) {
                            // Lưu lại vị trí của nút "Bắt đầu"
                            startButtonPosition = getNodeCenterPosition(startButton)
                            Thread.sleep(1000) // Sleep ngắn để tránh quá tải thao tác
                            continue
                        }
                    }
                }

                findCloseButton(root)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)


                val goButton = getGoButton(root)
                if (goButton != null) {
                    val fl = findPartialText(root, "Theo dõi")
                    if (fl != null) {
                        fl.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                    goButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Thread.sleep(5000)
                    continue
                }

                val xuStreamer = findText(root, "Xu Streamer")
                if (xuStreamer != null) {
                    val nhan = findPartialText(root, "Lưu")
                    if (nhan != null) {
                        nhan.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }

                    val countdownText = findCountdownNear(xuStreamer)
                    if (countdownText != null) {
                        val minutes = extractMinutes(countdownText)
                        if (minutes > 5) {
                            performScrollOrSwipe()
                        }
                    }
                } else {
                    performScrollOrSwipe()
                }
                Thread.sleep(3000)
            }
        }.start()
    }


    // New find start Button
    

    // Lấy vị trí trung tâm của node
    private fun getNodeCenterPosition(node: AccessibilityNodeInfo): Pair<Float, Float> {
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)
        return Pair(bounds.centerX().toFloat(), bounds.centerY().toFloat())
    }

    // Spam click vào vị trí đã lưu
    private fun spamClickAtPosition(x: Float, y: Float) {
        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50)) // Click trong 50ms
            .build()

        dispatchGesture(gesture, null, null)
    }
    // END

    // Close modal


    private fun closeModal(rootNode: AccessibilityNodeInfo) {
        // Tìm tất cả các nút với text "X" (hoặc mô tả tương tự)
        val closeButtons = rootNode.findAccessibilityNodeInfosByText("X")
        
        if (closeButtons.isNotEmpty()) {
            val closeButton = closeButtons[0]
            // Nếu nút đóng tồn tại, thực hiện click vào nó
            closeButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }
    // END

    // Find close Button
    fun findCloseButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val nodeList = ArrayList<AccessibilityNodeInfo>()
        findAllNodes(root, nodeList)

        val metrics = Resources.getSystem().displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val dpi = metrics.densityDpi

        // Chuyển 4cm thành pixel
        val minDistanceFromBottomPx = ((4f / 2.54f) * dpi).toInt()

        for (node in nodeList) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            val height = bounds.height()
            val width = bounds.width()

            val centerX = (bounds.left + bounds.right) / 2

            if (height < 200 && width < 200) {
                val isCenterHorizontally = centerX in (screenWidth / 4)..(screenWidth * 3 / 4)
                val isFarFromBottom = bounds.bottom < (screenHeight - minDistanceFromBottomPx)
                val isInLowerHalf = bounds.top > screenHeight / 2

                if (isCenterHorizontally && isFarFromBottom && isInLowerHalf) {
                    if (node.className == "android.widget.Button" || node.className == "android.widget.ImageView") {
                        return node
                    }
                }
            }
        }
        return null
    }

    private fun findAllNodes(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        list.add(node)
        for (i in 0 until node.childCount) {
            findAllNodes(node.getChild(i), list)
        }
    }

    // END

    private fun getGoButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val countdownNodes = mutableListOf<AccessibilityNodeInfo>()
        collectCountdownNodes(root, countdownNodes)

        if (countdownNodes.isEmpty()) return null

        val lastCountdown = countdownNodes[countdownNodes.size - 1]

        if (countdownNodes.size == 3 || (countdownNodes.size == 2 && findPartialText(root, "Xu Streamer") == null)) {
            return lastCountdown.parent
        }
        return null
    }

    private fun collectCountdownNodes(node: AccessibilityNodeInfo?, list: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return

        val text = node.text?.toString()
        if (text != null && text.matches(Regex("\\d{1,2}:\\d{2}"))) {
            list.add(node)
        }

        for (i in 0 until node.childCount) {
            collectCountdownNodes(node.getChild(i), list)
        }
    }

    private fun findText(node: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.text?.toString()?.contains(text) == true) return node
        for (i in 0 until node.childCount) {
            val result = findText(node.getChild(i), text)
            if (result != null) return result
        }
        return null
    }

    private fun findPartialText(node: AccessibilityNodeInfo?, partial: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.text?.toString()?.startsWith(partial) == true) return node
        for (i in 0 until node.childCount) {
            val result = findPartialText(node.getChild(i), partial)
            if (result != null) return result
        }
        return null
    }

    private fun performScrollOrSwipe() {
        val scrolled = performGlobalAction(4096)
        if (!scrolled) {
            swipeManually()
        }
    }

    private fun findCountdownNear(node: AccessibilityNodeInfo): String? {
        val parent = node.parent ?: return null
        for (i in 0 until parent.childCount) {
            val child = parent.getChild(i)
            val text = child?.text?.toString()
            if (text != null && text.matches(Regex("\\d{1,2}:\\d{2}"))) {
                return text
            }
        }
        return null
    }

    private fun extractMinutes(countdown: String): Int {
        val parts = countdown.split(":")
        return parts.getOrNull(0)?.toIntOrNull() ?: 0
    }

    private fun swipeManually() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val startX = (screenWidth / 2).toFloat()
        val startY = (screenHeight * 0.7).toFloat()
        val endY = (screenHeight * 0.4).toFloat()

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(startX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            .build()

        dispatchGesture(gesture, null, null)
    }
}
