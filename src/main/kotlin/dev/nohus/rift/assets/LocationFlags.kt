package dev.nohus.rift.assets

object LocationFlags {
    fun getName(flag: String): String? {
        return mapOf(
            "AssetSafety" to null, // Known from location
            "AutoFit" to null, // Inside a container
            "BoosterBay" to "Booster Hold",
            "Cargo" to "Cargo",
            "CorporationGoalDeliveries" to "Deliveries",
            "CorpseBay" to "Corpse Hold",
            "Deliveries" to "Deliveries",
            "DroneBay" to "Drone Bay",
            "FighterBay" to "Fighter Bay",
            "FighterTube0" to "Fighter Tube 0",
            "FighterTube1" to "Fighter Tube 1",
            "FighterTube2" to "Fighter Tube 2",
            "FighterTube3" to "Fighter Tube 3",
            "FighterTube4" to "Fighter Tube 4",
            "FleetHangar" to "Fleet Hangar",
            "FrigateEscapeBay" to "Frigate Escape Bay",
            "Hangar" to null, // Inside a hangar
            "HangarAll" to null,
            "HiSlot0" to "High Slot 0",
            "HiSlot1" to "High Slot 1",
            "HiSlot2" to "High Slot 2",
            "HiSlot3" to "High Slot 3",
            "HiSlot4" to "High Slot 4",
            "HiSlot5" to "High Slot 5",
            "HiSlot6" to "High Slot 6",
            "HiSlot7" to "High Slot 7",
            "HiddenModifiers" to "Hidden Modifiers",
            "Implant" to "Implant",
            "LoSlot0" to "Low Slot 0",
            "LoSlot1" to "Low Slot 1",
            "LoSlot2" to "Low Slot 2",
            "LoSlot3" to "Low Slot 3",
            "LoSlot4" to "Low Slot 4",
            "LoSlot5" to "Low Slot 5",
            "LoSlot6" to "Low Slot 6",
            "LoSlot7" to "Low Slot 7",
            "Locked" to "Locked",
            "MedSlot0" to "Medium Slot 0",
            "MedSlot1" to "Medium Slot 1",
            "MedSlot2" to "Medium Slot 2",
            "MedSlot3" to "Medium Slot 3",
            "MedSlot4" to "Medium Slot 4",
            "MedSlot5" to "Medium Slot 5",
            "MedSlot6" to "Medium Slot 6",
            "MedSlot7" to "Medium Slot 7",
            "MobileDepotHold" to "Mobile Depot Hold",
            "QuafeBay" to "Quafe Hold",
            "RigSlot0" to "Rig Slot 0",
            "RigSlot1" to "Rig Slot 1",
            "RigSlot2" to "Rig Slot 2",
            "RigSlot3" to "Rig Slot 3",
            "RigSlot4" to "Rig Slot 4",
            "RigSlot5" to "Rig Slot 5",
            "RigSlot6" to "Rig Slot 6",
            "RigSlot7" to "Rig Slot 7",
            "ShipHangar" to "Ship Hangar",
            "Skill" to "Skill",
            "SpecializedAmmoHold" to "Ammo Hold",
            "SpecializedAsteroidHold" to "Asteroid Hold",
            "SpecializedCommandCenterHold" to "Command Center Hold",
            "SpecializedFuelBay" to "Fuel Hold",
            "SpecializedGasHold" to "Gas Hold",
            "SpecializedIceHold" to "Ice Hold",
            "SpecializedIndustrialShipHold" to "Industrial Ship Hold",
            "SpecializedLargeShipHold" to "Large Ship Hold",
            "SpecializedMaterialBay" to "Material Hold",
            "SpecializedMediumShipHold" to "Medium Ship Hold",
            "SpecializedMineralHold" to "Mineral Hold",
            "SpecializedOreHold" to "Ore Hold",
            "SpecializedPlanetaryCommoditiesHold" to "Planetary Commodities Hold",
            "SpecializedSalvageHold" to "Salvage Hold",
            "SpecializedShipHold" to "Ship Hold",
            "SpecializedSmallShipHold" to "Small Ship Hold",
            "StructureDeedBay" to "Structure Deed Hold",
            "SubSystemBay" to "Subsystem Hold",
            "SubSystemSlot0" to "Subsystem 0",
            "SubSystemSlot1" to "Subsystem 1",
            "SubSystemSlot2" to "Subsystem 2",
            "SubSystemSlot3" to "Subsystem 3",
            "SubSystemSlot4" to "Subsystem 4",
            "SubSystemSlot5" to "Subsystem 5",
            "SubSystemSlot6" to "Subsystem 6",
            "SubSystemSlot7" to "Subsystem 7",
            "Unlocked" to null, // Inside an unlocked container
            "Wardrobe" to "Wardrobe",
        )[flag]
    }
}
