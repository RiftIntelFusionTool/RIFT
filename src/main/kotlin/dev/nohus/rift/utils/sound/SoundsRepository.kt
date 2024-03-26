package dev.nohus.rift.utils.sound

import org.koin.core.annotation.Single

@Single
class SoundsRepository {

    private val soundResources = listOf(
        Sound(id = 1, resource = "click.wav", name = "Notification 1"),
        Sound(id = 2, resource = "button.wav", name = "Notification 2"),
        Sound(id = 3, resource = "achieve.wav", "Notification 3"),
        Sound(id = 4, resource = "interface.wav", name = "Notification 4"),
        Sound(id = 5, resource = "pop.wav", name = "Pop"),
        Sound(id = 6, resource = "ding-idea.wav", name = "Ding"),
        Sound(id = 7, resource = "game-bonus.wav", name = "Bonus"),
        Sound(id = 8, resource = "swoosh.wav", name = "Swoosh"),
        Sound(id = 9, resource = "ambient-metal-whoosh.wav", "Metal whoosh"),
        Sound(id = 10, resource = "bell-transition.wav", name = "Magic sparkle"),
        Sound(id = 11, resource = "surprise.wav", name = "Surprise"),
        Sound(id = 12, resource = "power-up-sparkle.wav", name = "Power up"),
    )

    fun getSounds(): List<Sound> {
        return soundResources
    }
}
