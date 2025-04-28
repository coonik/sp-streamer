package com.streamerbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.res.Resources
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View

class MainService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        startChecking()
    }

    private fun startChecking() {
        Thread {
            while (true) {
                val root = rootInActiveWindow ?: continue
                root.refresh()

                val popup = findClickableNodeByText(root, "vòng quay")
                if (popup != null) {
                    clickLoop()
                    continue
                }

                findCloseButton(root)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)

                val goButton = getGoButton(root)
                if (goButton != null) {
                    findClickableNodeByText(root, "Theo dõi", exactMatch = true)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    goButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Thread.sleep(5000)
                    continue
                }

                val xuStreamer = findClickableNodeByText(root, "xu streamer")
                if (xuStreamer != null) {
                    findClickableNodeByText(root, "lưu")?.performAction(AccessibilityNodeInfo.ACTION_CLICK)

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

    private var isClicking = false

    private fun clickLoop() {
        // Nếu đang click thì không làm gì
        if (isClicking) return

        isClicking = true

        Thread {
            val metrics = Resources.getSystem().displayMetrics
            val screenWidth = metrics.widthPixels
            val screenHeight = metrics.heightPixels
            val dpi = metrics.densityDpi
            val cmToPx = dpi / 2.54f // 1cm = dpi/2.54

            val centerX = screenWidth / 2f
            val centerY = screenHeight - (8f * cmToPx)

            while (isClicking) {
                val currentRoot = rootInActiveWindow ?: continue
                currentRoot.refresh()

                val isFound = findClickableNodeByText(currentRoot, "vòng quay")
                if (isFound == null) {
                    isClicking = false // dừng khi không tìm thấy nút
                    break
                }

                // 2. Nếu còn thì tiếp tục click
                val path = Path().apply {
                    moveTo(centerX, centerY)
                }

                val gesture = GestureDescription.Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, 1))
                    .build()

                dispatchGesture(gesture, null, null)

                // Hiển thị hiệu ứng highlight sau mỗi lần click
                val highlightView = HighlightView(applicationContext)
                highlightView.setHighlight(centerX, centerY, 50f)  // 50f là bán kính highlight, có thể thay đổi

                // Delay giữa các lần click
                Thread.sleep(10) // click cực nhanh
            }
        }.start()
    }

    private fun findCloseButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val nodeList = ArrayList<AccessibilityNodeInfo>()
        findAllNodes(root, nodeList)

        val metrics = Resources.getSystem().displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        val dpi = metrics.densityDpi
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

    private fun getGoButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val countdownNodes = mutableListOf<AccessibilityNodeInfo>()
        collectCountdownNodes(root, countdownNodes)

        if (countdownNodes.isEmpty()) return null

        val lastCountdown = countdownNodes.last()

        if (countdownNodes.size == 3 || (countdownNodes.size == 2 && findClickableNodeByText(root, "xu streamer") == null)) {
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

    private fun performScrollOrSwipe() {
        val scrolled = performGlobalAction(4096)
        if (!scrolled) {
            swipeManually()
        }
    }

    private fun swipeManually() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val startX = (screenWidth / 2).toFloat()
        val startY = (screenHeight * 0.7f)
        val endY = (screenHeight * 0.4f)

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(startX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            .build()
        dispatchGesture(gesture, null, null)
    }

    private fun findClickableNodeByText(node: AccessibilityNodeInfo?, keyword: String, exactMatch: Boolean = false): AccessibilityNodeInfo? {
        if (node == null) return null

        val text = node.text?.toString()?.trim()
        val match = if (exactMatch) {
            text == keyword
        } else {
            text?.lowercase()?.contains(keyword.lowercase()) == true
        }

        if (match) {
            var clickableNode: AccessibilityNodeInfo? = node
            while (clickableNode != null && !clickableNode.isClickable) {
                clickableNode = clickableNode.parent
            }
            if (clickableNode != null && clickableNode.isVisibleToUser) {
                return clickableNode
            }
        }

        for (i in 0 until node.childCount) {
            val result = findClickableNodeByText(node.getChild(i), keyword, exactMatch)
            if (result != null) return result
        }

        return null
    }
}

class HighlightView(context: Context) : View(context) {
    private val paint = Paint().apply {
        color = 0x55FF0000 // đỏ nhạt
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private var highlightX = -1f
    private var highlightY = -1f
    private var highlightRadius = 0f

    fun setHighlight(x: Float, y: Float, radius: Float) {
        highlightX = x
        highlightY = y
        highlightRadius = radius
        invalidate()
    }

    fun clearHighlight() {
        highlightX = -1f
        highlightY = -1f
        highlightRadius = 0f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (highlightX >= 0 && highlightY >= 0 && highlightRadius > 0) {
            canvas.drawCircle(highlightX, highlightY, highlightRadius, paint)
        }
    }
}
