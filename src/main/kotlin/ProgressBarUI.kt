import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.GraphicsUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.TexturePaint
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.IOException
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.SwingConstants
import javax.swing.plaf.ComponentUI
import javax.swing.plaf.basic.BasicGraphicsUtils
import javax.swing.plaf.basic.BasicProgressBarUI

class ProgressBarUi : BasicProgressBarUI() {
    private var bimage: BufferedImage? = null
    override fun getPreferredSize(c: JComponent): Dimension {
        return Dimension(super.getPreferredSize(c).width, JBUIScale.scale(20))
    }

    override fun installListeners() {
        super.installListeners()
        progressBar.addComponentListener(object : ComponentAdapter() {
            override fun componentShown(e: ComponentEvent) {
                super.componentShown(e)
            }

            override fun componentHidden(e: ComponentEvent) {
                super.componentHidden(e)
            }
        })
    }

    @Volatile
    private var offset = 0

    @Volatile
    private var offset2 = 0

    @Volatile
    private var velocity = 1

    init {
        try {
            bimage = ImageIO.read(this.javaClass.getResource("/trace.png"))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun paintIndeterminate(g2d: Graphics, c: JComponent) {
        if (g2d !is Graphics2D) {
            return
        }
        val b = progressBar.insets
        val barRectWidth = progressBar.width - (b.right + b.left)
        val barRectHeight = progressBar.height - (b.top + b.bottom)
        if (barRectWidth <= 0 || barRectHeight <= 0) {
            return
        }
        g2d.color = JBColor(Gray._240.withAlpha(50), Gray._128.withAlpha(50))
        val w = c.width
        var h = c.preferredSize.height
        if (!isEven(c.height - h)) h++
        if (c.isOpaque) {
            g2d.fillRect(0, (c.height - h) / 2, w, h)
        }
        g2d.color = JBColor(Gray._165.withAlpha(50), Gray._88.withAlpha(50))
        val config = GraphicsUtil.setupAAPainting(g2d)
        g2d.translate(0, (c.height - h) / 2)
        val x = -offset
        val r = JBUIScale.scale(8f)
        val r2 = JBUIScale.scale(9f)
        val containingRoundRect = Area(RoundRectangle2D.Float(1f, 1f, w - 2f, h - 2f, r, r))
        g2d.fill(containingRoundRect)
        offset = (offset + 1) % periodLength
        offset2 += velocity
        if (offset2 <= 2) {
            offset2 = 2
            velocity = 1
        } else if (offset2 >= w - JBUIScale.scale(15)) {
            offset2 = w - JBUIScale.scale(15)
            velocity = -1
        }
        val area = Area(Rectangle2D.Float(0f, 0f, w.toFloat(), h.toFloat()))
        area.subtract(Area(RoundRectangle2D.Float(1f, 1f, w - 2f, h - 2f, r, r)))
        if (c.isOpaque) {
            g2d.fill(area)
        }
        area.subtract(Area(RoundRectangle2D.Float(0f, 0f, w.toFloat(), h.toFloat(), r2, r2)))
        val parent = c.parent
        if (c.isOpaque) {
            g2d.fill(area)
        }
        if (velocity > 0) {
            Icons.ITMO.paintIcon(progressBar, g2d, offset2 - JBUIScale.scale(5), -JBUIScale.scale(2))
        } else {
            Icons.ITMO.paintIcon(progressBar, g2d, offset2 - JBUIScale.scale(5), -JBUIScale.scale(2))
        }
        g2d.draw(RoundRectangle2D.Float(1f, 1f, w - 2f - 1f, h - 2f - 1f, r, r))
        g2d.translate(0, -(c.height - h) / 2)
        if (progressBar.isStringPainted) {
            if (progressBar.orientation == SwingConstants.HORIZONTAL) {
                paintString(g2d, b.left, b.top, barRectWidth, barRectHeight, boxRect.x, boxRect.width)
            } else {
                paintString(g2d, b.left, b.top, barRectWidth, barRectHeight, boxRect.y, boxRect.height)
            }
        }
        config.restore()
    }

    override fun paintDeterminate(g: Graphics, c: JComponent) {
        if (g !is Graphics2D) {
            return
        }
        if (progressBar.orientation != SwingConstants.HORIZONTAL || !c.componentOrientation.isLeftToRight) {
            super.paintDeterminate(g, c)
            return
        }
        val config = GraphicsUtil.setupAAPainting(g)
        val b = progressBar.insets // area for border
        val w = progressBar.width
        var h = progressBar.preferredSize.height
        if (!isEven(c.height - h)) h++
        val barRectWidth = w - (b.right + b.left)
        val barRectHeight = h - (b.top + b.bottom)
        if (barRectWidth <= 0 || barRectHeight <= 0) {
            return
        }
        val amountFull = getAmountFull(b, barRectWidth, barRectHeight)
        val parent = c.parent
        val background = if (parent != null) parent.background else UIUtil.getPanelBackground()
        g.setColor(background)
        if (c.isOpaque) {
            g.fillRect(0, 0, w, h)
        }
        val r = JBUIScale.scale(8f)
        val r2 = JBUIScale.scale(9f)
        val off = JBUIScale.scale(1f)
        g.translate(0, (c.height - h) / 2)
        g.color = progressBar.foreground
        g.fill(RoundRectangle2D.Float(0f, 0f, w - off, h - off, r2, r2))
        g.color = background
        g.fill(RoundRectangle2D.Float(off, off, w - 2f * off - off, h - 2f * off - off, r, r))
        if (bimage != null) {
            val tp = TexturePaint(
                bimage,
                Rectangle2D.Double(0.0, 1.0, (h - 2f * off - off).toDouble(), (h - 2f * off - off).toDouble())
            )
            g.paint = tp
        }
        g.fill(
            RoundRectangle2D.Float(
                2f * off,
                2f * off,
                amountFull - JBUIScale.scale(5f),
                h - JBUIScale.scale(5f),
                JBUIScale.scale(7f),
                JBUIScale.scale(7f)
            )
        )
        Icons.ITMO.paintIcon(progressBar, g, amountFull - JBUIScale.scale(5), -JBUIScale.scale(2))
        g.translate(0, -(c.height - h) / 2)
        if (progressBar.isStringPainted) {
            paintString(
                g, b.left, b.top,
                barRectWidth, barRectHeight,
                amountFull, b
            )
        }
        config.restore()
    }

    private fun paintString(g: Graphics, x: Int, y: Int, w: Int, h: Int, fillStart: Int, amountFull: Int) {
        if (g !is Graphics2D) {
            return
        }
        val progressString = progressBar.string
        g.font = progressBar.font
        var renderLocation = getStringPlacement(
            g, progressString,
            x, y, w, h
        )
        val oldClip = g.clipBounds
        if (progressBar.orientation == SwingConstants.HORIZONTAL) {
            g.color = selectionBackground
            BasicGraphicsUtils.drawString(
                progressBar,
                g,
                progressString,
                renderLocation.x.toFloat(),
                renderLocation.y.toFloat()
            )
            g.color = selectionForeground
            g.clipRect(fillStart, y, amountFull, h)
            BasicGraphicsUtils.drawString(
                progressBar,
                g,
                progressString,
                renderLocation.x.toFloat(),
                renderLocation.y.toFloat()
            )
        } else {
            g.color = selectionBackground
            val rotate = AffineTransform.getRotateInstance(Math.PI / 2)
            g.font = progressBar.font.deriveFont(rotate)
            renderLocation = getStringPlacement(
                g, progressString,
                x, y, w, h
            )
            BasicGraphicsUtils.drawString(
                progressBar,
                g,
                progressString,
                renderLocation.x.toFloat(),
                renderLocation.y.toFloat()
            )
            g.color = selectionForeground
            g.clipRect(x, fillStart, w, amountFull)
            BasicGraphicsUtils.drawString(
                progressBar,
                g,
                progressString,
                renderLocation.x.toFloat(),
                renderLocation.y.toFloat()
            )
        }
        g.clip = oldClip
    }

    override fun getBoxLength(availableLength: Int, otherDimension: Int): Int {
        return availableLength
    }

    private val periodLength: Int
        get() = JBUIScale.scale(16)

    companion object {
        fun createUI(c: JComponent): ComponentUI {
            c.border = JBUI.Borders.empty().asUIResource()
            return ProgressBarUi()
        }

        private fun isEven(value: Int): Boolean {
            return value % 2 == 0
        }
    }
}