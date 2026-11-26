package dev.jeppu.domain.product;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Base64;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Encodes and decodes the opaque pagination cursor.
 *
 * <p>The cursor is the sort key of the last item returned ({@code {"v":1,"code":"P109"}}),
 * Base64-encoded so that clients treat it as a black box. The version field ({@code v})
 * allows the format to evolve without breaking already-issued cursors.
 */
@Component
class ProductCursorCodec {

    /** Version marker for the cursor format; bump when the payload shape changes. */
    private static final int CURSOR_VERSION = 1;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    String encode(String lastCode) {
        try {
            String json = jsonMapper.writeValueAsString(new CursorPayload(CURSOR_VERSION, lastCode));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
        } catch (Exception e) {
            // Never happens for a two-field record, but a checked-API wrapper keeps the caller simple.
            throw new IllegalStateException("Failed to encode cursor", e);
        }
    }

    /** Decodes a cursor or returns null if absent; throws {@link InvalidCursorException} on garbage. */
    String decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String json = new String(Base64.getUrlDecoder().decode(cursor));
            CursorPayload payload = jsonMapper.readValue(json, CursorPayload.class);
            if (payload.version() != CURSOR_VERSION) {
                throw new InvalidCursorException("Unsupported cursor version");
            }
            return payload.code();
        } catch (InvalidCursorException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidCursorException("Malformed cursor");
        }
    }

    private record CursorPayload(
            @JsonProperty("v") int version,
            @JsonProperty("code") String code) {}
}
