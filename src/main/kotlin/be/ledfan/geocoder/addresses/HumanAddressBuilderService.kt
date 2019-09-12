package be.ledfan.geocoder.addresses

import be.ledfan.geocoder.db.entity.AddressIndex
import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.db.mapper.AddressIndexMapper
import be.ledfan.geocoder.importer.core.TagParser

class HumanAddressBuilderService(private val addressIndexMapper: AddressIndexMapper) {

    fun build(preferredLanguages: LinkedHashSet<String>, addressIndex: AddressIndex): String {
        if (!addressIndex.relationsFetched) {
            addressIndexMapper.fetchRelations(addressIndex)
        }

        var address = ""

        addressIndex.entity?.let {
            // name of Address/Venue
            nameOfEntity(preferredLanguages, it)?.let { name ->
                address += "$name, "
            }
        }

        addressIndex.street?.let {
            nameOfEntity(preferredLanguages, it)?.let { name ->
                address += name
            }
        }

        with(addressIndex.housenumber) {
            if (this != null) {
                address += " $this"
            }
            address += ", "
        }

        addressIndex.neighbourhood?.let {
            nameOfEntity(preferredLanguages, it)?.let { name ->
                address += "$name, "
            }
        }

        addressIndex.localAdmin?.let {
            nameOfEntity(preferredLanguages, it)?.let { name ->
                address += "$name, "
            }
        }

        addressIndex.county?.let {
            nameOfEntity(preferredLanguages, it)?.let { name ->
                address += "$name, "
            }
        }

        addressIndex.macroregion?.let {
            nameOfEntity(preferredLanguages, it)?.let { name ->
                address += "$name, "
            }
        }

        addressIndex.country?.let {
            nameOfEntity(preferredLanguages, it)?.let { name ->
                address += name
            }
        }

        return address
    }

    fun nameOfEntity(preferredLanguages: LinkedHashSet<String>, entity: OsmEntity): String? {
        // TODO Fix langcode
        if (!entity.parsedTags.hasChild("name")) {
            return null
        }
        var name: String? = null
        val nameTag = entity.parsedTags.child("name")

        if (nameTag.amountOfChildren > 0) {
            for (possibleLang in preferredLanguages) {
                if (nameTag.hasChild(possibleLang)) {
                    name = nameTag.child(possibleLang).singleValueOrNull()
                    if (name != null) break
                }
            }
        }

        return name ?: nameTag.singleValueOrNull()
    }


}