package dev.gbppimentel.snapcompanion.next;
import org.json.*;
/** Human-readable observation history; internal payloads stay out of the normal UI. */
final class LiveEventText {
    static String describe(JSONObject event,Catalog catalog){
        JSONObject fields=event.optJSONObject("fields");if(fields==null)return "Observation details unavailable";
        String subject="Subject not identified";Catalog.Species species=catalog.species(fields.optInt("subject"));if(species!=null)subject=species.name();
        return switch(event.optString("type")){
            case "possible-shot"->"Possible photo taken · film "+fields.optInt("before")+" → "+fields.optInt("after")+"\n"+subject+". A film decrease does not confirm a successful photo category.";
            case "evaluation-candidate"->subject+" comparison · "+fields.optInt("previousTotal")+" → "+fields.optInt("total")+" points\nBoth score-component sums checked. Submission remains unconfirmed.";
            case "request-candidate"->{String id=fields.optString("request"),name=catalog.requests.stream().filter(r->r.id().equals(id)).map(Catalog.Request::title).findFirst().orElse("Request not identified");yield "Possible request result: "+name+"\nCompletion text observed repeatedly; confirm only after checking the game.";}
            default->"Screen observation";
        };
    }
}
