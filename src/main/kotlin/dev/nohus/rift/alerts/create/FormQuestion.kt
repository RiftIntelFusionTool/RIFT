package dev.nohus.rift.alerts.create

import dev.nohus.rift.utils.sound.Sound

sealed class FormQuestion(
    open val title: String,
) {
    data class SingleChoiceQuestion(
        override val title: String,
        val items: List<FormChoiceItem>,
    ) : FormQuestion(title)

    data class MultipleChoiceQuestion(
        override val title: String,
        val items: List<FormChoiceItem>,
    ) : FormQuestion(title)

    data class SystemQuestion(
        override val title: String,
        val allowEmpty: Boolean,
    ) : FormQuestion(title)

    data class JumpsRangeQuestion(
        override val title: String,
    ) : FormQuestion(title)

    data class OwnedCharacterQuestion(
        override val title: String,
    ) : FormQuestion(title)

    data class IntelChannelQuestion(
        override val title: String,
    ) : FormQuestion(title)

    data class SoundQuestion(
        override val title: String,
    ) : FormQuestion(title)

    data class SpecificCharactersQuestion(
        override val title: String,
        val allowEmpty: Boolean,
    ) : FormQuestion(title)

    data class CombatTargetQuestion(
        override val title: String,
        val placeholder: String,
        val allowEmpty: Boolean,
    ) : FormQuestion(title)

    data class PlanetaryIndustryColoniesQuestion(
        override val title: String,
    ) : FormQuestion(title)

    data class FreeformTextQuestion(
        override val title: String,
        val placeholder: String,
        val allowEmpty: Boolean,
    ) : FormQuestion(title)
}

data class FormChoiceItem(
    val id: Int,
    val text: String,
    val description: String? = null,
)

sealed interface FormAnswer {
    data class SingleChoiceAnswer(
        val id: Int,
    ) : FormAnswer

    data class MultipleChoiceAnswer(
        val items: List<FormChoiceItem>,
    ) : FormAnswer {
        val ids get() = items.map { it.id }
    }

    data class SystemAnswer(
        val system: String,
    ) : FormAnswer

    data class JumpsRangeAnswer(
        val minJumps: Int,
        val maxJumps: Int,
    ) : FormAnswer

    data class CharacterAnswer(
        val characterId: Int,
    ) : FormAnswer

    data class IntelChannelAnswer(
        val channel: String,
    ) : FormAnswer

    sealed interface SoundAnswer : FormAnswer {
        data class BuiltInSound(
            val item: Sound,
        ) : SoundAnswer

        data class CustomSound(
            val path: String,
        ) : SoundAnswer
    }

    data class SpecificCharactersAnswer(
        val characters: List<String>,
    ) : FormAnswer

    data class PlanetaryIndustryColoniesAnswer(
        val colonies: List<String>,
    ) : FormAnswer

    data class FreeformTextAnswer(
        val text: String,
    ) : FormAnswer
}
