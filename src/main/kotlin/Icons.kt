import java.util.*
import javax.swing.ImageIcon

interface Icons {
    companion object {
        val ITMO = ImageIcon(
            Objects.requireNonNull(
                Icons::class.java.getResource("/itmo/itmoIcon.svg")
            )
        )
    }
}