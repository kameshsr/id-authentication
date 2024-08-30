package io.mosip.authentication.common.service.util;

import io.mosip.authentication.common.service.config.IDAMappingConfig;
import io.mosip.authentication.common.service.helper.IdentityAttributesForMatchTypeHelper;
import io.mosip.authentication.common.service.helper.SeparatorHelper;
import io.mosip.authentication.core.exception.IdAuthenticationBusinessException;
import io.mosip.authentication.core.indauth.dto.IdentityInfoDTO;
import io.mosip.authentication.core.spi.indauth.match.IdInfoFetcher;
import io.mosip.authentication.core.spi.indauth.match.MatchType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.mosip.authentication.core.constant.IdAuthCommonConstants.LANG_CODE_SEPARATOR;

@Component
public class EntityInfoUtil {

    @Autowired
    private SeparatorHelper separatorHelper;

    @Autowired
    private LanguageUtil computeKeyHelper;

    /** The id info fetcher. */
    @Autowired
    private IdInfoFetcher idInfoFetcher;

    /** The id mapping config. */
    @Autowired
    private IDAMappingConfig idMappingConfig;

    @Autowired
    private IdentityAttributesForMatchTypeHelper identityAttributesForMatchTypeHelper;

    /**
     * Gets the entity info map.
     *
     * @param matchType     the match type
     * @param identityInfos the id entity
     * @param language the language
     * @param idName the id name
     * @return the entity info map
     * @throws IdAuthenticationBusinessException the id authentication business exception
     */
    public Map<String, String> getIdEntityInfoMap(MatchType matchType, Map<String, List<IdentityInfoDTO>> identityInfos,
            String language, String idName) throws IdAuthenticationBusinessException {
        List<String> propertyNames = identityAttributesForMatchTypeHelper.getIdentityAttributesForMatchType(matchType, idName);
        Map<String, String> identityValuesMapWithLang = getIdentityValuesMap(matchType, propertyNames, language, identityInfos);
        Map<String, String> identityValuesMapWithoutLang = getIdentityValuesMap(matchType, propertyNames, null, identityInfos);
        Map<String, String> mergedMap = mergeNonNullValues(identityValuesMapWithLang, identityValuesMapWithoutLang);
        Map<String, Object> props = Map.of(IdInfoFetcher.class.getSimpleName(), idInfoFetcher);
        return matchType.getEntityInfoMapper().apply(mergedMap, props);
    }

    /**
     * Gets the entity info map.
     *
     * @param matchType     the match type
     * @param identityInfos the id entity
     * @param language the language
     * @return the entity info map
     * @throws IdAuthenticationBusinessException the id authentication business exception
     */
    public Map<String, String> getIdEntityInfoMap(MatchType matchType, Map<String, List<IdentityInfoDTO>> identityInfos,
                                                         String language) throws IdAuthenticationBusinessException {
        return getIdEntityInfoMap(matchType, identityInfos, language, null);
    }

    /**
     * Gets the identity values map.
     *
     * @param matchType the match type
     * @param propertyNames the property names
     * @param languageCode  the language code
     * @param idEntity the id entity
     * @return the identity values map
     * @throws IdAuthenticationBusinessException the id authentication business exception
     */
    private Map<String, String> getIdentityValuesMap(MatchType matchType, List<String> propertyNames,
                                                     String languageCode, Map<String, List<IdentityInfoDTO>> idEntity) throws IdAuthenticationBusinessException {
        Map<String, Map.Entry<String, List<IdentityInfoDTO>>> mappedIdEntity = matchType.mapEntityInfo(idEntity,
                idInfoFetcher);
        Function<? super String, ? extends String> keyMapper = propName -> {
            String key = mappedIdEntity.get(propName).getKey();
            if (languageCode != null) {
                key = key + LANG_CODE_SEPARATOR + languageCode;
            }
            return key;
        };
        Function<? super String, ? extends String> valueMapper = propName -> getIdentityValueFromMap(propName,
                languageCode, mappedIdEntity, matchType).findAny().orElse("");

        return propertyNames.stream()
                .filter(propName -> mappedIdEntity.containsKey(propName))
                .collect(
                        Collectors.toMap(keyMapper, valueMapper, (p1, p2) -> p1, () -> new LinkedHashMap<String, String>()));
    }

    /**
     * Merge non null values.
     *
     * @param map1 the identity values map
     * @param map2 the identity values map without lang
     * @return
     */
    private Map<String, String> mergeNonNullValues(Map<String, String> map1, Map<String, String> map2) {
        Predicate<? super Map.Entry<String, String>> nonNullPredicate = entry -> entry.getValue() != null && !entry.getValue().trim().isEmpty();
        Map<String, String> mergeMap = map1.entrySet()
                .stream()
                .filter(nonNullPredicate)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (m1, m2) -> m1,  () -> new LinkedHashMap<>()));
        map2.entrySet()
                .stream()
                .filter(nonNullPredicate)
                .forEach(entry -> mergeMap.merge(entry.getKey(), entry.getValue(), (str1, str2) -> str1));
        return mergeMap;
    }

    /**
     * Fetch the identity value.
     *
     * @param name                 the name
     * @param languageForMatchType the language for match type
     * @param identityInfo         the demo info
     * @param matchType the match type
     * @return the identity value
     */
    private Stream<String> getIdentityValueFromMap(String name, String languageForMatchType,
                                                   Map<String, Map.Entry<String, List<IdentityInfoDTO>>> identityInfo, MatchType matchType) {
        List<IdentityInfoDTO> identityInfoList = identityInfo.get(name).getValue();
        if (identityInfoList != null && !identityInfoList.isEmpty()) {
            return identityInfoList.stream()
                    .filter(idinfo -> (languageForMatchType == null && !matchType.isPropMultiLang(name, idMappingConfig))
                            || idInfoFetcher.checkLanguageType(languageForMatchType, idinfo.getLanguage()))
                    .map(idInfo -> idInfo.getValue());
        }
        return Stream.empty();
    }


    /**
     * Gets the entity info as string.
     *
     * @param matchType  the match type
     * @param idEntity the id entity
     * @return the entity info as string
     * @throws IdAuthenticationBusinessException the id authentication business exception
     */
    public String getEntityInfoAsString(MatchType matchType, Map<String, List<IdentityInfoDTO>> idEntity)
            throws IdAuthenticationBusinessException {
        return getEntityInfoAsString(matchType, null, idEntity);
    }

    /**
     * Gets the entity info as string.
     *
     * Note: This method is not used during authentication match, so the
     * separator used in concatenation will not be used during the match.
     *
     * @param matchType the match type
     * @param langCode  the lang code
     * @param idEntity  the id entity
     * @return the entity info as string
     * @throws IdAuthenticationBusinessException the id authentication business
     *                                           exception
     */
    public String getEntityInfoAsString(MatchType matchType, String langCode,
                                               Map<String, List<IdentityInfoDTO>> idEntity) throws IdAuthenticationBusinessException {
        Map<String, String> entityInfo = getEntityInfoAsStringWithKey(matchType, langCode,
                        idEntity, null);
        if(entityInfo == null || entityInfo.isEmpty()) {
            return null;
        }
        return entityInfo.values().iterator().next();
    }


/**
        * Retrieves entity information as a string map with a computed key.
 *
         * @param matchType The type of match to perform
 * @param langCode The language code for localization
 * @param idEntity A map of identity information
 * @param key An optional key to use; if null, the actual key will be used
 * @return A map containing entity information as a string value
 * @throws IdAuthenticationBusinessException If authentication fails
 */
    public Map<String, String> getEntityInfoAsStringWithKey(MatchType matchType, String langCode,
                                                            Map<String, List<IdentityInfoDTO>> idEntity, String key) throws IdAuthenticationBusinessException {
        Map<String, String> entityInfoMap = getIdEntityInfoMap(matchType, idEntity, langCode);
        if(entityInfoMap == null || entityInfoMap.isEmpty()) {
            return Map.of();
        }
        String actualKey = entityInfoMap.keySet().iterator().next();
        return Map.of(key == null ? actualKey :  computeKeyHelper.computeKey(key, entityInfoMap.keySet().iterator().next(), langCode) ,concatValues(separatorHelper.getSeparator(matchType.getIdMapping().getIdname()), entityInfoMap.values().toArray(new String[entityInfoMap.size()])));
    }

    /**
     * Concat values.
     *
     * @param values the values
     * @return the string
     */
    private String concatValues(String sep, String... values) {
        StringBuilder demoBuilder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            String demo = values[i];
            if (null != demo && demo.length() > 0) {
                demoBuilder.append(demo);
                if (i < values.length - 1) {
                    demoBuilder.append(sep);
                }
            }
        }
        return demoBuilder.toString();
    }
}
