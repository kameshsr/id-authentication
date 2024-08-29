package io.mosip.authentication.common.service.helper;

import io.mosip.authentication.common.service.util.EntityInfoUtil;
import io.mosip.authentication.core.constant.IdAuthCommonConstants;
import io.mosip.authentication.core.constant.IdAuthenticationErrorConstants;
import io.mosip.authentication.core.exception.IdAuthenticationBusinessException;
import io.mosip.authentication.core.indauth.dto.AuthRequestDTO;
import io.mosip.authentication.core.indauth.dto.IdentityInfoDTO;
import io.mosip.authentication.core.logger.IdaLogger;
import io.mosip.authentication.core.spi.indauth.match.*;
import io.mosip.kernel.core.logger.spi.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 @author Kamesh Shekhar Prasad
 */

@Component
public class MatchTypeHelper {

    @Autowired
    private IdInfoFetcher idInfoFetcher;

    /** The mosip logger. */
    private static Logger mosipLogger = IdaLogger.getLogger(MatchTypeHelper.class);

    @Autowired
    private EntityInfoUtil entityInfoUtil;

    /**
     * Match type.
     *
     * @param authRequestDTO the auth request DTO
     * @param idEntity     the id entity
     * @param input          the input
     * @param partnerId the partner id
     * @return the match output
     * @throws IdAuthenticationBusinessException the id authentication business
     *                                           exception
     */
    public MatchOutput matchType(AuthRequestDTO authRequestDTO, Map<String, List<IdentityInfoDTO>> idEntity,
            MatchInput input, String partnerId) throws IdAuthenticationBusinessException {
        return matchType(authRequestDTO, idEntity, "", input, (t, m, p) -> null, partnerId);
    }

    /**
     * Match type.
     *
     * @param authRequestDTO the id DTO
     * @param idEntity     the id entity
     * @param uin the uin
     * @param input          the input
     * @param entityValueFetcher the entity value fetcher
     * @param partnerId the partner id
     * @return the match output
     * @throws IdAuthenticationBusinessException the id authentication business
     *                                           exception
     */
    public MatchOutput matchType(AuthRequestDTO authRequestDTO, Map<String, List<IdentityInfoDTO>> idEntity,
            String uin, MatchInput input, EntityValueFetcher entityValueFetcher, String partnerId)
            throws IdAuthenticationBusinessException {
        String matchStrategyTypeStr = input.getMatchStrategyType();
        if (matchStrategyTypeStr == null) {
            matchStrategyTypeStr = MatchingStrategyType.EXACT.getType();
        }

        Optional<MatchingStrategyType> matchStrategyType = MatchingStrategyType
                .getMatchStrategyType(matchStrategyTypeStr);
        if (matchStrategyType.isPresent()) {
            MatchingStrategyType strategyType = matchStrategyType.get();
            MatchType matchType = input.getMatchType();
            Optional<MatchingStrategy> matchingStrategy = matchType.getAllowedMatchingStrategy(strategyType);
            if (matchingStrategy.isPresent()) {
                MatchingStrategy strategy = matchingStrategy.get();
                Map<String, String> reqInfo = null;
                reqInfo = getAuthReqestInfo(matchType, authRequestDTO);
                String idName = input.getIdName();
                if (null == reqInfo || reqInfo.isEmpty()) {
                    reqInfo = idInfoFetcher.getIdentityRequestInfo(matchType, idName, authRequestDTO.getRequest(),
                            input.getLanguage());
                }
                if (null != reqInfo && reqInfo.size() > 0) {

                    Map<String, Object> matchProperties = input.getMatchProperties();

                    Map<String, String> entityInfo = getEntityInfo(idEntity, uin, authRequestDTO, input,
                            entityValueFetcher, matchType, strategy, idName, partnerId);

                    int mtOut = strategy.match(reqInfo, entityInfo, matchProperties);
                    boolean matchOutput = mtOut >= input.getMatchValue();
                    return new MatchOutput(mtOut, matchOutput, input.getMatchStrategyType(), matchType,
                            input.getLanguage(), idName);
                }
            } else {
                // FIXME Log that matching strategy is not allowed for the match type.
                mosipLogger.info(IdAuthCommonConstants.SESSION_ID, "Matching strategy >>>>>: " + strategyType,
                        " is not allowed for - ", matchType + " MatchType");
            }

        }
        return null;
    }

    /**
     * Construct match type.
     *
     * @param idEntity         the id entity
     * @param uin                the uin
     * @param req the req
     * @param input              the input
     * @param entityValueFetcher the entity value fetcher
     * @param matchType          the match type
     * @param strategy           the strategy
     * @param idName the id name
     * @param partnerId the partner id
     * @return the match output
     * @throws IdAuthenticationBusinessException the id authentication business
     *                                           exception
     */
    private Map<String, String> getEntityInfo(Map<String, List<IdentityInfoDTO>> idEntity,
                                              String uin,
                                              AuthRequestDTO req,
                                              MatchInput input,
                                              EntityValueFetcher entityValueFetcher,
                                              MatchType matchType,
                                              MatchingStrategy strategy,
                                              String idName,
                                              String partnerId)
            throws IdAuthenticationBusinessException {

        Map<String, String> entityInfo = null;
        if (matchType.hasRequestEntityInfo()) {
            entityInfo = entityValueFetcher.fetch(uin, req, partnerId);
        } else if (matchType.hasIdEntityInfo()) {
            entityInfo = entityInfoUtil.getIdEntityInfoMap(matchType, idEntity, input.getLanguage(), idName);
        } else {
            entityInfo = Collections.emptyMap();
        }

        if (null == entityInfo || entityInfo.isEmpty()
                || entityInfo.entrySet().stream().anyMatch(value -> value.getValue() == null
                || value.getValue().isEmpty() || value.getValue().trim().length() == 0)) {
            switch (matchType.getCategory()) {
                case BIO:
                    throw new IdAuthenticationBusinessException(
                            IdAuthenticationErrorConstants.BIOMETRIC_MISSING.getErrorCode(),
                            String.format(IdAuthenticationErrorConstants.BIOMETRIC_MISSING.getErrorMessage(),
                                    input.getAuthType().getType()));
                case DEMO:
                    if(null == input.getLanguage())  {
                        throw new IdAuthenticationBusinessException(
                                IdAuthenticationErrorConstants.DEMO_MISSING.getErrorCode(),
                                String.format(IdAuthenticationErrorConstants.DEMO_MISSING.getErrorMessage(),
                                        idName));
                    }
                    else {
                        throw new IdAuthenticationBusinessException(
                                IdAuthenticationErrorConstants.DEMO_MISSING_LANG.getErrorCode(),
                                String.format(IdAuthenticationErrorConstants.DEMO_MISSING_LANG.getErrorMessage(),
                                        idName, input.getLanguage()));
                    }
                case KBT:
                    throw new IdAuthenticationBusinessException(
                            IdAuthenticationErrorConstants.KEY_BINDING_MISSING.getErrorCode(),
                            String.format(IdAuthenticationErrorConstants.KEY_BINDING_MISSING.getErrorMessage(),
                                    input.getAuthType().getType()));

                case PWD:
                    throw new IdAuthenticationBusinessException(
                            IdAuthenticationErrorConstants.PASSWORD_MISSING.getErrorCode(),
                            String.format(IdAuthenticationErrorConstants.PASSWORD_MISSING.getErrorMessage(),
                                    input.getAuthType().getType()));
            }
        }
        return entityInfo;
    }

    /**
     * Get Authrequest Info.
     *
     * @param matchType the match type
     * @param authRequestDTO the auth request DTO
     * @return the auth reqest info
     */
    public Map<String, String> getAuthReqestInfo(MatchType matchType, AuthRequestDTO authRequestDTO) {
        return matchType.getReqestInfoFunction().apply(authRequestDTO);
    }

    /**
     * Match type.
     *
     * @param authRequestDTO     the auth request DTO
     * @param uin                the uin
     * @param input              the input
     * @param entityValueFetcher the entity value fetcher
     * @param partnerId the partner id
     * @return the match output
     * @throws IdAuthenticationBusinessException the id authentication business
     *                                           exception
     */
    MatchOutput matchType(AuthRequestDTO authRequestDTO, String uin, MatchInput input,
                          EntityValueFetcher entityValueFetcher, String partnerId) throws IdAuthenticationBusinessException {
        return matchType(authRequestDTO, Collections.emptyMap(), uin, input, entityValueFetcher, partnerId);
    }
}
