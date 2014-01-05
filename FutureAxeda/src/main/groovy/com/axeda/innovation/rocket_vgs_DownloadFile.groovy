import org.apache.commons.io.IOUtils

import static com.axeda.sdk.v2.dsl.Bridges.*
import javax.activation.MimetypesFileTypeMap
import com.axeda.drm.sdk.scripto.Request
 
 def response
 def contentType
 
 try {
     def knowntypes = [
             [png: 'image/png']
             ,[gif: 'image/gif']
             ,[jpg: 'image/jpeg']
             ,[wav: 'audio/wav']
             ,[amr: 'audio/amr-wb']
             ,[mp3: 'audio/mp3']
             ,[ogg: 'video/ogg']
             ,[vorbis: 'audio/vorbis']
             ,[mpeg: 'video/mpeg']
             ,[mp4: 'video/mp4']
             ,[mpeg: 'video/mpeg']
             ,[quicktime: 'video/quicktime']
             ,[flv: 'video/x-flv']
             ,[wmv: 'video/x-ms-wmv']
     ]

 
def params = Request.parameters.size() > 0 ? Request.parameters : parameters
 
response = fileInfoBridge.getFileData(params.fileId)
 
def fileinfo = fileInfoBridge.findById(params.fileId)
 
def type = fileinfo.filename.substring(fileinfo.filename.indexOf('.') + 1,fileinfo.filename.size())
 
type = returnType(knowntypes, type)
 
contentType = params.type ?: (type ?: 'image/jpg')
 
logger.info(contentType)
}
catch (Exception e){
    logger.info(e.localizedMessage)   
}
 
return ['Content': getBytes(response), 'Content-Disposition': contentType, 'Content-Type':contentType]

static byte[] getBytes(InputStream is) throws IOException {
	ByteArrayOutputStream answer = new ByteArrayOutputStream();
    IOUtils.copy(is,answer)
	return answer.toByteArray();
  }

def returnType(knowntypes, ext){
  return knowntypes.find{ it.containsKey(ext) }?."$ext"
}
