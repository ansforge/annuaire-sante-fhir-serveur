package fr.ans.afas.mdbexpression.domain.fhir;

import com.mongodb.client.model.Filters;
import fr.ans.afas.fhirserver.search.FhirSearchPath;
import fr.ans.afas.fhirserver.search.config.SearchConfigService;
import fr.ans.afas.fhirserver.search.exception.BadConfigurationException;
import fr.ans.afas.fhirserver.search.expression.ExpressionContext;
import fr.ans.afas.fhirserver.search.expression.HasCondition;
import org.bson.conversions.Bson;
import org.springframework.util.StringUtils;

public class MongoDbChainedReferenceExpression extends MongoDbReferenceExpression {

    private final HasCondition<Bson> hasCondition;

    public MongoDbChainedReferenceExpression(SearchConfigService searchConfigService, FhirSearchPath fhirPath, HasCondition<Bson> hasCondition) {
        // Pour le constructeur de la référence classique, on peut passer null pour le type/id car ils seront déterminés par le _has
        super(searchConfigService, fhirPath, null, null);
        this.hasCondition = hasCondition;
    }

    @Override
    public Bson interpreter(ExpressionContext context) {
        var config = searchConfigService.getSearchConfigByPath(fhirPath);
        if (config.isEmpty()) {
            throw new BadConfigurationException("Search not supported on path: " + fhirPath);
        }
        if (StringUtils.hasLength(this.type)) {
            return Filters.eq(context.getPrefix() + config.get().getIndexName() + REFERENCE_DB_SUFFIX, this.type + "/" + this.id);
        }else if(fhirPath.getChain()==null){
            return Filters.eq(context.getPrefix() + config.get().getIndexName() + ID_DB_SUFFIX, this.id);
        }
        return null;
    }

    @Override
    public HasCondition<Bson> getHasCondition() {
        return hasCondition;
    }


}