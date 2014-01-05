import org.apache.commons.io.IOUtils

import static com.axeda.sdk.v2.dsl.Bridges.*
import javax.activation.MimetypesFileTypeMap
import com.axeda.drm.sdk.scripto.Request
 
def parameters = [:]
def params = Request.parameters.size() > 0 ? Request.parameters : parameters

params.each() { k,v ->
    logger.info "Found Param: $k, with value $v"
}

def headers = Request.headers
headers.each() { k,v ->
    logger.info "Found Header: $k, with value $v"
}

def attachments = Request.attachments
attachments.each() { k,v ->
    logger.info "Attachment: $k"
}

return ["Content":"Done.","Content-Type":"text/plain"]
