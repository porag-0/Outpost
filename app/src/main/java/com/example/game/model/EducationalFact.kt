package com.example.game.model

data class EducationalFact(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val detailedScientificExplanation: String,
    val missionReference: String,
    val keyTakeaway: String
)

object EducationalDatabase {
    val facts = listOf(
        EducationalFact(
            id = "sabatier_oxygen",
            title = "ISRU: Sabatier Reactor & Water Electrolysis",
            category = "Life Support & Chemistry",
            summary = "How astronauts generate breathable oxygen and water without relying on supply rockets from Earth.",
            detailedScientificExplanation = "In Situ Resource Utilization (ISRU) is paramount for deep-space colonization. The Sabatier reaction combines carbon dioxide (CO2 exhaled by astronauts) with hydrogen (H2) at 300–400°C over a nickel or ruthenium catalyst to produce methane (CH4) and water (H2O):\n\nCO₂ + 4H₂ → CH₄ + 2H₂O\n\nThe produced water is then routed through an electrolysis unit (2H₂O → 2H₂ + O₂), separating pure breathable oxygen for the outpost while cycling hydrogen back into the reactor. Solid oxide electrolysis can also extract oxygen directly from atmospheric CO₂.",
            missionReference = "MOXIE Experiment & Space Station ECLSS Architecture",
            keyTakeaway = "Every cubic meter of oxygen produced locally saves thousands of dollars in rocket payload mass."
        ),
        EducationalFact(
            id = "regolith_sintering",
            title = "Lunar & Martian Regolith 3D Sintering",
            category = "Civil & Materials Engineering",
            summary = "Constructing radiation-proof habitats using native planetary soil instead of transporting concrete.",
            detailedScientificExplanation = "Planetary regolith is composed of crushed basalt, anorthosite, and sharp glass shards from billions of years of meteorite impacts. By focusing high-power microwaves, infrared lasers, or concentrated solar mirrors onto raw regolith, engineers can induce sintering—fusing mineral particles just below their melting point without needing water or binding agents.\n\n3D autonomous robotic arms layer molten regolith into interlocking geodesic vault blocks, providing a 2 to 3-meter thick shell that shields crew habitats from galactic cosmic rays (GCR), solar particle events, and micrometeoroids.",
            missionReference = "Lunar Surface Innovation Initiative & 3D Regolith Additive Manufacturing",
            keyTakeaway = "Native regolith shielding stops up to 90% of lethal cosmic ionizing radiation."
        ),
        EducationalFact(
            id = "kilopower_nuclear",
            title = "Small Fission Surface Nuclear Power",
            category = "Power & Thermal Systems",
            summary = "Continuous megawatt power during the 14-day freezing lunar night and blinding dust storms.",
            detailedScientificExplanation = "The lunar night lasts 354 hours (~14.5 Earth days) with temperatures plunging to -173°C (-280°F), where photovoltaic solar panels generate zero power. While batteries are too heavy for weeks of continuous baseline power, deep space engineers developed the Kilopower reactor project.\n\nUsing an enriched uranium solid reactor core cooled by sodium heat pipes, thermal energy transfers to high-efficiency Stirling engines, converting heat directly into 10 to 40 kilowatts of uninterrupted electrical energy for at least 10 years without maintenance.",
            missionReference = "Kilopower KRUSTY Fission Surface Power Project",
            keyTakeaway = "Nuclear fission provides reliable baseload power independent of dust storms or celestial orbital darkness."
        ),
        EducationalFact(
            id = "closed_loop_greenhouse",
            title = "Controlled Ecological Life Support Systems (CELSS)",
            category = "Astrobiology & Agriculture",
            summary = "Growing nutrient-dense fresh food in microgravity and hypobaric controlled chambers.",
            detailedScientificExplanation = "Transporting pre-packaged rations introduces nutrient degradation—especially Vitamins B1 and C over multi-year journeys. CELSS utilizes closed-loop nutrient film hydroponics and aeroponics (suspending roots in mist rather than soil) inside sealed pressurized greenhouses.\n\nCrops like dwarf wheat, radishes, sweet potatoes, and microgreens absorb crew-exhaled CO2 and transpire purified humidity, which condensers capture as potable drinking water. Specialized LED spectra (rich in 450nm royal blue and 660nm deep red photons) maximize photosynthetic photon flux density while saving electrical power.",
            missionReference = "Orbital Veggie & Advanced Plant Habitat (APH) Research",
            keyTakeaway = "A 20-square-meter aeroponic greenhouse recycles 98% of plant transpiration water while feeding astronauts fresh nutrients."
        ),
        EducationalFact(
            id = "rocker_bogie",
            title = "Rocker-Bogie Planetary Rover Suspension",
            category = "Robotics & Mobility",
            summary = "Why deep space and planetary rovers have six wheels that never flip over even on boulder fields.",
            detailedScientificExplanation = "Standard automobile suspensions use springs that cause bouncing, instability, and tilt in low gravity. The Rocker-Bogie mechanism consists of two 'rocker' arms connected across a central differential pivot, with a smaller 'bogie' attached to each side.\n\nThis clever mechanical linkage ensures that all six driven wheels maintain constant contact with the regolith simultaneously when climbing obstacles larger than the wheel diameter itself (such as 40cm boulders or 45-degree crater rims), distributing vehicle weight evenly and preventing rollover.",
            missionReference = "Deep Space Rover Mobility Systems & VIPER Lunar Traversal",
            keyTakeaway = "Independent six-wheel drive with differential rocker linkage enables safe traversal of treacherous unpaved terrain."
        )
    )
}
