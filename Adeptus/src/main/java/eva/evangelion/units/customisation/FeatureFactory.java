package eva.evangelion.units.customisation;

import java.util.ArrayList;
import java.util.List;
import static eva.evangelion.units.customisation.Upgrade.*;
import static eva.evangelion.units.customisation.Feature.*;

public class FeatureFactory {
    public static List<Feature> getAllFeatures() {
        List<Feature> features = new ArrayList<>();

        // ========== HISTORY ==========
        features.add(new Feature.FeatureBuilder(
                "REDACTED",
                "Your Eva's history has been systematically erased. Despite being an older model, " +
                        "\nthere is not one file, document, or reference on it before it was transferred to your posting. " +
                        "\nWeird. No effect.",
                FeatureType.HISTORY, 1, 5)
                .addTag(Tag.NO_EFFECT)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Field Testing",
                "This Evangelion has been selected to give crucial testing to experimental prototype weapons." +
                        "\nChoose three Technologies. You do not need to pay the Requisition cost to apply those Technologies to a weapon." +
                        "\nHowever, other Technologies have their Requisition Cost increased to 2.",
                FeatureType.HISTORY, 6, 15)
                .addTag(Tag.REQUISITION)
                .requisition(0)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Resurrected",
                "During initial testing, there was a cataclysmic core failure that nearly destroyed the Eva entirely" +
                        "\nThe Eva has been stitched back together with synthetic parts, but at a Cost.\n+1 Armor, -1 Toughness.",
                FeatureType.HISTORY, 16, 25)
                .addTag(Tag.STATS)
                .stat(Stat.ARMOR, 1)
                .stat(Stat.TOUGHNESS, -1)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Outside Funding",
                "Your Evangelion was paid for by some other group, be it a corporation or a country or some less public organization." +
                        "\nThis organization outfits your Evangelion well, but Nerv has had precious little time to improve the Eva." +
                        "\nYou gain 2 Requisition, but begin play with only 2 Upgrades of your choice.",
                FeatureType.HISTORY, 26, 35)
                .addTag(Tag.REQUISITION)
                .requisition(2)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Angelic Core",
                "Something about your Eva's core is in tune with the Angels you fight, and this insight spills over into your head during contact with the enemy." +
                        "\nOnce per combat, you can gain 1 Doom to use the Divine Strength Angel Power, replacing your current Technology for this attack.",
                FeatureType.HISTORY, 36, 45)
                .addTag(Tag.POWER)
                .powerLevel(1)
                .doom(1)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Echo",
                "They say that this Eva had another pilot before you, a test pilot who died in the plug somehow. You certainly feel like there is someone else in there with you, sometimes." +
                        "\nVoices. Flashes of memories that are not yours.\nOnce per battle you can use a 0 Stamina Reaction to immediately recover your ATP. " +
                        "\nIn the Preparation Phase of every Battle, you gain 1 Doom.",
                FeatureType.HISTORY, 46, 55)
                .addTag(Tag.POWER)
                .addTag(Tag.ADD_DOOM)
                .powerLevel(1)
                .doom(1)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Relentless",
                "The Eva has a particular hatred for the enemy that drives it to seek out and destroy the Angels at all costs.\n" +
                        "Nerv is at a loss to explain this incredible behavior.\nYou increase your Speed by 1 but you do not gain any bonus Reflexes from the Run Action.",
                FeatureType.HISTORY, 56, 65)
                .addTag(Tag.COMPLEX)
                .stat(Stat.SPEED, 1)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Ancient",
                "Rumor has it that, crazy as it sounds, the majority of your Eva was simply dug out of the ground rather than built." +
                        "\nBut that’s just nonsense, right?" +
                        "\nUnarmed and AT Power Attacks gain +1 Damage, but all other attacks have a -1 damage Penalty.",
                FeatureType.HISTORY, 66, 75)
                .addTag(Tag.COMPLEX)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Science Project",
                "Your Eva was originally intended for research purposes, not actual combat use. You have only one Wing on an arm of your choosing." +
                        "\nYour other Arm houses complex analytical equipment which improves your final Collateral Rating per battle by 10 for the purposes of Research.\n" +
                        "If multiple Evas have this Feature, each additional Eva only improves Collateral by +1.",
                FeatureType.HISTORY, 76, 85)
                .addTag(Tag.WING_REPLACEMENT)
                .addTag(Tag.POST_BATTLE_EFFECT)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Signature Weapon",
                "Due to UN meddling, your Evangelion was designed from the ground up with a paired weapon instead of the more broad Anti-Angel arsenal of Nerv." +
                        "\nChoose a Special Weapon that costs 2 or 3 Requisition.\nYou may deploy with this weapon for 0 Requisition, but you also begin play with 0 Requisition.",
                FeatureType.HISTORY, 86, 95)
                .addTag(Tag.REQUISITION)
                .addTag(Tag.WEAPON)
                .requisition(0)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Destined to Meet",
                "This Eva seems strangely suited to you.\nPick one other History and keep it.",
                FeatureType.HISTORY, 96, 100)
                .addTag(Tag.PICK)
                .build());

        // ========== EXPERIMENTAL ==========
        features.add(new Feature.FeatureBuilder(
                "Back to the Drawing Board",
                "Whatever they tried to do, it didn’t work." +
                        "\nDespite the amazing claims in your documentation, the Eva doesn’t perform any better or any worse than normal." +
                        "\nNo effect.",
                FeatureType.EXPERIMENTAL, 1, 8)
                .addTag(Tag.NO_EFFECT)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Bonded",
                "The Eva has taken a liking to you; no other pilot can operate your Evangelion—not even a dummy plug." +
                        "\nOnce per Battle, before making a Guard Test, you can choose to succeed as if you rolled a 1." +
                        "\nHowever, on the next Turn after using this feature, the Evangelion rampages out of your control and spends 1 Stamina to make a melee Basic Attack on the Angel," +
                        "\nregardless of your current equipment or situation. If you are not in range to use a melee attack," +
                        "\nit also spends 1 Stamina to Run into melee range, or as close as possible to the Angel.",
                FeatureType.EXPERIMENTAL, 9, 16)
                .addTag(Tag.POWER)
                .addTag(Tag.FORCED_ACTION)
                .powerLevel(1)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Armor Lattice",
                "The Eva’s armor is honeycombed with structural supports and crumple zones, " +
                        "\ncapable of taking an impressive amount of damage, but leaving the Eva more vulnerable than ever afterwards." +
                        "\nOnce per battle, you may reduce the damage of an attack made against you by 50%." +
                        "\nAfter the attack is resolved, your Armor is reduced by 2 for the rest of the Battle.",
                FeatureType.EXPERIMENTAL, 17, 23)
                .addTag(Tag.POWER)
                .powerLevel(1)
                .stat(Stat.ARMOR, -2)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Stimulant Injector",
                "Syringes filled with an unethical amount of stimulants are integrated into the Entry Plug." +
                        "\nWhen you become Bruised, you can instead choose to become Wounded, but you only take half the normal penalty from this Injury Condition.",
                FeatureType.EXPERIMENTAL, 24, 31)
                .addTag(Tag.POWER)
                .powerLevel(1)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Type-2 Jericho Modification",
                "One of the Evangelion’s Wings has been permanently replaced by large, fixed shield." +
                        "\nYou gain +1 Toughness and +5 Reflexes.",
                FeatureType.EXPERIMENTAL, 32, 38)
                .addTag(Tag.WING_REPLACEMENT)
                .addTag(Tag.STATS)
                .stat(Stat.TOUGHNESS, 1)
                .stat(Stat.REFLEXES, 5)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Overspecialized",
                "The Eva was designed to utilize a very specific strategy—to the exclusion of all others." +
                        "\nChoose up to 3 Standard Weapons. You have a +5 bonus to Attack Tests and deal +1 Damage with these weapons," +
                        "\nbut all others suffer a -10 Penalty to Attack Tests and a -1 penalty to Damage. Wing Loadout Upgrades are exempt from this penalty.",
                FeatureType.EXPERIMENTAL, 39, 46)
                .addTag(Tag.COMPLEX)
                .stat(Stat.ACCURACY, 5)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Psychoactive Frame",
                "This Eva’s is designed with a unique support system, using the AT Field itself as a weapon.\n\nYou do not gain the Layered Field Spread Pattern, but you may choose 2 AT Power Upgrades and begin play with those.",
                FeatureType.EXPERIMENTAL, 47, 54)
                .addTag(Tag.REQUISITION)
                .requisition(0)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Edged Armor",
                "The Evangelion’s armor plating has been altered to include several spikes or bladed fins to assist in close quarters combat, however this design is less protective than hoped.\n\nThe Evangelion has -1 Armor, but you begin play with the Natural Weapon Upgrade.",
                FeatureType.EXPERIMENTAL, 55, 61)
                .addTag(Tag.UPGRADE)
                .addTag(Tag.STATS)
                .stat(Stat.ARMOR, -1)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Limiter Release",
                "The Eva is designed to operate at unsafe capacity in short bursts.\n\nYou can use a 0 Stamina Action on your Turn to gain 1 Stamina, up to the maximum of 2 Stamina. At the end of your Turn, you increase your Damage Pool by 4d3.",
                FeatureType.EXPERIMENTAL, 62, 68)
                .addTag(Tag.POWER)
                .addTag(Tag.COMPLEX)
                .powerLevel(2)
                .build());

        features.add(new Feature.FeatureBuilder(
                "X19 Organ",
                "The X19 Organ is an observed mutation in Evangelions that heightens aggression in the pilot.\n\nYou gain +1 Attack Strength, but your undisciplined movement always triggers Attacks of Opportunity, even if another effect would prevent it.",
                FeatureType.EXPERIMENTAL, 69, 76)
                .addTag(Tag.COMPLEX)
                .addTag(Tag.STATS)
                .stat(Stat.STRENGTH, 1)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Field Neutralizer",
                "The Evangelion is outfitted with exotic particle generators, flushing the area around it and weakening nearby AT Fields.\n\nOnce per Battle you can use this device as a 0 Stamina Reaction to interrupt an Angel Power, causing them to spend any Stamina or ATP to no effect. After using this ability, there is a 50% chance for you to spend your ATP to no effect at the beginning of your Turn.",
                FeatureType.EXPERIMENTAL, 77, 84)
                .addTag(Tag.POWER)
                .addTag(Tag.COMPLEX)
                .powerLevel(1)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Multipylon",
                "The Evangelion is weighed down by a single, wide structure across its shoulders.\n\nThe Eva has an additional Wing on their Body, but has -10 Reflexes.",
                FeatureType.EXPERIMENTAL, 85, 91)
                .addTag(Tag.WING)
                .addTag(Tag.STATS)
                .stat(Stat.REFLEXES, -10)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Work Order",
                "Make a formal request to the engineering team.\n\nPick one other Experimental and keep it.",
                FeatureType.EXPERIMENTAL, 92, 100)
                .addTag(Tag.PICK)
                .build());

        // ========== CONSTRUCTION ==========
        features.add(new Feature.FeatureBuilder(
                "Tactical Loadout",
                "The Eva is outfitted with a stylish attachment that can hold a single magazine of Ammo or Small Weapon, just like the Wing Loadout (Storage) Upgrade.",
                FeatureType.CONSTRUCTION, 1, 7)
                .addTag(Tag.WING)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Auto-Balancer",
                "The Eva has an advanced system designed to keep it balanced and upright.\n\nThe Eva can Stand as part of another movement Action.",
                FeatureType.CONSTRUCTION, 8, 14)
                .addTag(Tag.COMPLEX)
                .build());

        features.add(new Feature.FeatureBuilder(
                "HeavyArmor",
                "The armor on this Evangelion may not be any stronger, but there sure is a lot of it.\n\nThe Eva gains +1 Armor.",
                FeatureType.CONSTRUCTION, 15, 21)
                .addTag(Tag.STATS)
                .stat(Stat.ARMOR, 1)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Shock Armor",
                "The Evangelion is outfitted with a special series of superconductive plates.\n\nWhile you are Grabbing an enemy or you are Grabbed, you can take a 1 Stamina Action to Attack with your Shock Armor, which deals 1d10+S Damage and has the Superconductive Property.",
                FeatureType.CONSTRUCTION, 22, 28)
                .addTag(Tag.POWER)
                .addTag(Tag.COMPLEX)
                .powerLevel(1)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Sleek",
                "This Eva was built with fluid motion and flexibility in mind.\n\nYou gain +1 Speed.",
                FeatureType.CONSTRUCTION, 29, 35)
                .addTag(Tag.STATS)
                .stat(Stat.SPEED, 1)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Link System",
                "Your Eva has a real time feed to the ground support crews.\n\nYou gain an additional 2 Nerv Resources which can only be used by you.",
                FeatureType.CONSTRUCTION, 36, 42)
                .addTag(Tag.NERV_PERSONAL)
                .nervPersonal(2)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Emergency Override",
                "The Eva has been upgraded with an improved synapse system that lessens the burden on the pilot.\n\nLimit Cut now only has a 30% chance to Bruise you.",
                FeatureType.CONSTRUCTION, 43, 49)
                .addTag(Tag.COMPLEX)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Magnetic Lure",
                "You can use a Simple Action to pick up Evangelion weapons from up to 3 Sectors Away.\n\nOther items may be eligible for this Feature at GM discretion.",
                FeatureType.CONSTRUCTION, 50, 56)
                .addTag(Tag.POWER)
                .powerLevel(1)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Magi Terminal",
                "Your Eva houses a Magi Terminal, and your combat data is used in the Research and Development department.\n\nYou gain an additional 1d3 Research after each Battle. If multiple Eva have this Feature, it does not stack, but each additional Eva allows you to reroll the die.",
                FeatureType.CONSTRUCTION, 57, 63)
                .addTag(Tag.POST_BATTLE_EFFECT)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Advanced HUD",
                "The Entry plug setup of this Eva is unique, assisting with weapon targeting.\n\nTwice per Battle, you may reroll a Ranged Attack and take the new result.",
                FeatureType.CONSTRUCTION, 64, 70)
                .addTag(Tag.POWER)
                .powerLevel(2)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Well Equipped",
                "This Eva is provisioned by a sponsor, or perhaps just another Nerv Base with a better R&D budget.\n\nThe Eva starts with an additional Requisition.",
                FeatureType.CONSTRUCTION, 71, 77)
                .addTag(Tag.REQUISITION)
                .requisition(1)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Enhanced Suit",
                "This Eva came with a specialized plugsuit to suit the pilot's needs.\n\nBegin play with the Enhanced Plugsuit upgrade.",
                FeatureType.CONSTRUCTION, 78, 84)
                .addTag(Tag.UPGRADE)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Full Integration",
                "This Eva has a backup weapon placed inside one of its body parts, just in case.\n\nChoose an arm, the legs, or the head. That body part benefits from the Integrated Weapon Upgrade, even if there are no open Wings on it.",
                FeatureType.CONSTRUCTION, 85, 91)
                .addTag(Tag.COMPLEX)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Explosive Armor",
                "Once per Battle, you may detonate explosives lining your Evangelion’s outer armor as a 2 Stamina Action.\n\nThis is a Basic Attack with the Area (1) Property, and deals 2d6+2+S Damage.",
                FeatureType.CONSTRUCTION, 92, 100)
                .addTag(Tag.POWER)
                .powerLevel(1)
                .build());

        // ========== MUTATION ==========
        features.add(new Feature.FeatureBuilder(
                "Necrotic",
                "The Evangelion's flesh looks and smells like something dead.\n\nThe Eva gains +1 Toughness.",
                FeatureType.MUTATION, 1, 7)
                .addTag(Tag.STATS)
                .stat(Stat.TOUGHNESS, 1)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Giant",
                "The Evangelion beneath the armor is a powerful beast, noticeably stockier than a normal Eva.\n\nThe Eva gains +1 Attack Strength.",
                FeatureType.MUTATION, 8, 14)
                .addTag(Tag.STATS)
                .stat(Stat.STRENGTH, 1)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Runt",
                "Smaller and sleeker than other Evas, this Eva is easily missed. Literally.\n\n+5 Reflexes.",
                FeatureType.MUTATION, 15, 21)
                .addTag(Tag.STATS)
                .stat(Stat.REFLEXES, 5)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Spinneret",
                "The Eva can produce ropes of spider-silk to snare the Angel.\n\nYou can Grab with a range of 3.",
                FeatureType.MUTATION, 22, 28)
                .addTag(Tag.COMPLEX)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Extraneous Limbs",
                "Your Eva was developed with multiple additional limbs to aid them in Battle.\n\nFunctionally, you have a Third Arm hit location, which can only be struck on doubles results (11, 44, 77, etc). This Third Arm can attack with and hold one-handed items only, and cannot benefit from special properties on those items such as Defensive or those derived from Upgrades.",
                FeatureType.MUTATION, 29, 35)
                .addTag(Tag.COMPLEX)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Savage Feast",
                "The Evangelion is red in tooth and claw.\n\nOnce per Battle, you may make a Basic Attack against the Angel using your Unarmed Attack. If you hit, you can reduce your damage pool by 2d6.",
                FeatureType.MUTATION, 36, 42)
                .addTag(Tag.POWER)
                .powerLevel(1)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Acid Blood",
                "What flows through this Eva’s veins is highly caustic.\n\nWhen at the Wound Level 2 or greater, the Angel increases its Damage Pool by 2 every time they hit you with a Melee Attack.",
                FeatureType.MUTATION, 43, 49)
                .addTag(Tag.COMPLEX)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Pack Hunter",
                "The Eva can let loose a howl that seems to increase the responsiveness of other Evas.\n\nOnce per Battle, you can use a 0 Stamina Action to grant you and all allies within 3 Sectors a +5 bonus to Accuracy and a +1 bonus to Damage. This effect lasts until the end of your next Turn.",
                FeatureType.MUTATION, 50, 56)
                .addTag(Tag.POWER)
                .powerLevel(1)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Regeneration",
                "This Eva heals faster than normal, sometimes fast enough to see it happen.\n\nThe Eva begins play with the Spontaneous Regeneration upgrade and may use it twice per Battle.",
                FeatureType.MUTATION, 57, 63)
                .addTag(Tag.UPGRADE)
                .addTag(Tag.POWER)
                .powerLevel(2)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Predatory",
                "The Evas is especially dangerous up close, a true monster.\n\nTwice per Battle, you may reroll a Melee Attack.",
                FeatureType.MUTATION, 64, 70)
                .addTag(Tag.POWER)
                .powerLevel(2)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Hard to Kill",
                "Whether due to iron-hard skin or unnaturally good luck, this Eva takes a lot of punishment.\n\nThe Eva may use the Redundant Organs upgrade twice per Battle.",
                FeatureType.MUTATION, 71, 77)
                .addTag(Tag.POWER)
                .powerLevel(2)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Extending Arms",
                "Once per Battle, you may grant any melee weapon you are using the Reach property as a 0 Stamina Action.\n\nIt lasts until the start of your next Turn.",
                FeatureType.MUTATION, 78, 84)
                .addTag(Tag.POWER)
                .powerLevel(1)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Long Stride",
                "The Evangelion’s legs are long and lanky.\n\nYou may move an additional Sector with the Maneuver Action.",
                FeatureType.MUTATION, 85, 91)
                .addTag(Tag.COMPLEX)
                .build());

        features.add(new Feature.FeatureBuilder(
                "Atomic Breath",
                "The Evangelion can break the restraints on its jaw and let loose a giant ball of plasma from within.\n\nYou gain the Sacred Cross AT Power.",
                FeatureType.MUTATION, 92, 100)
                .addTag(Tag.POWER)
                .powerLevel(1)
                .build());

        // ========== COSMETIC ==========
        String[] cosmeticNames = {
                "Cranial Horn", "Monoeye", "Clawed Fingers", "Hunched Posture",
                "Vertical Eyes", "Extra Eyes", "Ornament", "Patriotic",
                "Glowing Eyes", "Spinal Fins", "Weak Tail", "Twitch",
                "Bulldog", "Bizarre", "Horns", "Webbed Fingers",
                "Rusty", "Vent", "Venus", "Blindsight",
                "Exposed Eyes", "Segmented Armor", "Mucus", "Luminescent Blood",
                "Gangly"
        };
        int[] starts = {1,5,9,13,17,21,25,29,33,37,41,45,49,53,57,61,65,69,73,77,81,85,89,93,97};
        int[] ends = {4,8,12,16,20,24,28,32,36,40,44,48,52,56,60,64,68,72,76,80,84,88,92,96,100};
        String[] descriptions = {
                "The Eva has a single horn protruding from its forehead.",
                "The Eva has a single, clearly artificial, sensory apparatus in place of its eyes.",
                "The Eva's fingers end in what look like wicked claws.",
                "The Eva is always slouching, as if carrying a great weight.",
                "The eyes of the Eva run vertical rather than horizontal.",
                "The Eva has between 3 and 6 eyes.",
                "The Eva has a useless, but aesthetically pleasing, artifact attached to its face or chest.",
                "The Eva has one or more symbols of the country which made it plastered on its body.",
                "The Eva's eyes glow with an unhealthy color when active.",
                "The Eva has a series of metal protrusions that jut out from the back.",
                "The Eva has a short, useless tail of some kind.",
                "Sometimes, even when offline, the Eva’s fingers move slightly.",
                "The Eva has wider than normal shoulders, giving it a brutish appearance.",
                "Roll twice on this table and take both results, rerolling any Bizarre results.",
                "The Eva has noticeable, bony protrusions on its head.",
                "There is a thin, durable membrane between the fingers of the Eva's hands.",
                "The armor of the Eva, while plenty functional, seems old and rusty.",
                "The Eva has a tendency to vent small gusts of steam or smoke.",
                "The Eva has wider than normal hips, giving it a feminine appearance.",
                "The Eva has no discernible eyes of any kind.",
                "The Eva's eyes are exposed and lidless, the eyeball plainly visible and Bloodshot.",
                "The Eva's armor has large gaps in it, exposing the tough flesh beneath.",
                "The Evangelion tends to drip an unidentified substance from beneath its Armor.",
                "The Eva's blood softly glows the same color as its Secondary Color.",
                "The Eva’s limbs are disproportionately long for its frame."
        };

        for (int i = 0; i < cosmeticNames.length; i++) {
            Feature.FeatureBuilder builder = new Feature.FeatureBuilder(
                    cosmeticNames[i],
                    descriptions[i],
                    FeatureType.COSMETIC,
                    starts[i], ends[i]
            );
            if (cosmeticNames[i].equals("Bizarre")) {
                builder.addTag(Tag.COMPLEX);
            } else {
                builder.addTag(Tag.NO_EFFECT);
            }
            features.add(builder.build());
        }

        return features;
    }
}