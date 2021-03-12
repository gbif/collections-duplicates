# Project: Collections registry (GRSciColl) duplicate removal

Project that retrieves collections duplicates from the registry API and creates Github issues to handle each duplicate group separately.
The first round of issue generated correspond to institutions that have the same name and are located in the same city: https://github.com/gbif/collections-duplicates/issues/192

# How can you help?

Anyone can review [the issues created](https://github.com/gbif/collections-duplicates/issues) and give us some input.
When commenting, it would help us if you could:
* Let us know whether the potential duplicates listed are actual duplicates or should remain separate entries.
* Specify whether you are affiliated with the institution(s) concerned.
* Include in your comment some references (links to the institution websites, documentation, etc.)

## To keep in mind

Here a a few points to keep in mind in order to help you review potential duplicates:
1. **Is the entry "valid"**? The "valid" field in GRSciColl indicates whether a particular institution/collection and its code are deprecated or not. When merging an institution or collection with another one, its code will become and alternative code in the remaining entry and can still be used to be linked with specimens. So unless the institution explicitly ask the two entries to remain separate, it seems safe to merge an "invalid" entry with a valid one.
2. **Which entry is linked with specimens?** As you might know, the occurrences on GBIF are linked to GRSciColl whenever possible. Keeping that in mind can help visualize which collection or institution codes and identifiers are used and chose the institution to keep.
3. **Are the entries coming from Index Herbariorum synchronization?** The IH synchronization generates a lot of duplicates as several Herbaria can belong to the same institution.
4. **In doubt, check with the institution.** They will know how they want to be represented on GRSciColl.


## For the GRSciColl editors

GRSciColl editors can then resolve the duplicates highlighted in the GBIF registry by using the merge function.

Remember:
* That not all the editors can merge all entities (iDigBio entities require special permissions for example).
* That when merging an entry with an other one, all the identifiers, code, associated collections and staff will be transfered to the entry kept. More information [here](https://github.com/gbif/registry/issues/255).
* Two Index Herbariorum entries cannot be merged with each other (one has to be disconnected from IH first). More information on how IH is connected to GRSciColl [here](https://github.com/gbif/registry/issues/167).
* Two collection entries can only be merged if they belong to the same institution.
