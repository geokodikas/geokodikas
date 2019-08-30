package be.ledfan.geocoder.addresses

import be.ledfan.geocoder.db.entity.AddressIndex
import be.ledfan.geocoder.db.entity.OsmEntity
import be.ledfan.geocoder.db.mapper.AddressIndexMapper
import be.ledfan.geocoder.importer.core.TagParser

class HumanAddressBuilderService(
        private val addressIndexMapper: AddressIndexMapper,
        private val tagParser: TagParser) {

    fun build(langCode: LangCode, addressIndex: AddressIndex): String {
        if (!addressIndex.relationsFetched) {
            addressIndexMapper.fetchRelations(addressIndex)
        }

        var address = ""

        // TODO name of building

        addressIndex.street?.let {
            nameOfEntity(langCode, it)?.let { name ->
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
            nameOfEntity(langCode, it)?.let { name ->
                address += "$name, "
            }
        }

        addressIndex.localAdmin?.let {
            nameOfEntity(langCode, it)?.let { name ->
                address += "$name, "
            }
        }

        addressIndex.county?.let {
            nameOfEntity(langCode, it)?.let { name ->
                address += "$name, "
            }
        }

        addressIndex.macroregion?.let {
            nameOfEntity(langCode, it)?.let { name ->
                address += "$name, "
            }
        }

        addressIndex.country?.let {
            nameOfEntity(langCode, it)?.let { name ->
                address += name
            }
        }

        return address
    }

    fun nameOfEntity(langCode: LangCode, entity: OsmEntity): String? {
        if (!entity.parsedTags.hasChild("name")) {
            return null
        }
        var name: String? = null
        val nameTag = entity.parsedTags.child("name")

        if (nameTag.amountOfChildren > 0 && nameTag.hasChild(langCode.identifier)) {
            name = nameTag.child(langCode.identifier).singleValueOrNull()
        }

        return name ?: nameTag.singleValueOrNull()
    }


}