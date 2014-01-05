import com.axeda.drm.sdk.Context
import com.axeda.drm.sdk.data.*
import com.axeda.drm.sdk.device.*
import com.axeda.drm.sdk.mobilelocation.CurrentMobileLocationFinder
import com.axeda.drm.sdk.mobilelocation.MobileLocation
import com.axeda.drm.sdk.mobilelocation.MobileLocationFinder
import com.axeda.sdk.v2.bridge.FileInfoBridge
import static com.axeda.sdk.v2.dsl.Bridges.*
import com.axeda.services.v2.ExecutionResult
import com.axeda.services.v2.FileInfo
import com.axeda.services.v2.FileInfoReference
import com.axeda.services.v2.FileUploadSession
import com.axeda.services.v2.FileInfoCriteria
import net.sf.json.JSONObject
import groovy.json.JsonBuilder
import net.sf.json.JSONArray
import com.axeda.drm.sdk.scripto.Request
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.exception.ExceptionUtils
import com.axeda.common.sdk.id.Identifier
import com.axeda.services.v2.ExternalCredentialCriteria
import com.axeda.services.v2.NamedValue
import com.axeda.services.v2.ExtendedMap
import com.axeda.services.v2.ExtendedMapCriteria
import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import groovy.json.*
import javax.imageio.ImageIO;
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream;
import java.awt.*
import java.awt.geom.*
import javax.imageio.*
import java.awt.image.*
import java.awt.Graphics2D
import javax.imageio.stream.ImageInputStream
 
/*
   FileStore entry point to post and store files
*/
 
def contentType = "application/json"
final def serviceName = "vgs_StoreFile"
 
// Create a JSON Builder
def json = new JsonBuilder()
 
// Global try/catch. Gotta have it, you never know when your code will be exceptional!
 
try {
    
    Context CONTEXT = Context.getSDKContext()
    def filesList = []
    def datestring = new Date().time
    InputStream inputStream = Request.inputStream
    
    def reqbody = Request.body
 
    // all of our Request Parameters are available here
    def params = Request.parameters
	def host = Request.headers.host.replace("https://", "").replace("http://", "")
    def filename = Request?.headers?.'Content-Disposition' ? 
    Request?.headers?.'Content-Disposition' : "file___" + datestring + ".txt"
    def filelabel = Request.parameters.filelabel ?: filename
    def description = Request.parameters.description ?: filename
    def contType = Request.headers?."content-type" ?: "image/jpeg"
    def tag = Request.parameters.tag ?: "vgsfile"
	def imgfilebool
    def encoded = Request.parameters.encoded?.toBoolean()
    byte[] bytes
    
    if (inputStream == null){
        throw new Exception("$serviceName: Input Stream is null")
    }
    
    // get an inputStream of the POSTed content (great for binary)
 
    def outerMap = [:]
 
        if (inputStream.available() > 0) {
            
            def fileext = filename.substring(filename.indexOf(".") + 1,filename.size())
            imgfilebool = fileext ==~ /((?i)(jpg|jpeg|png|gif|bmp)$)/
            def scaledImg
            /*
            if (imgfilebool){
				try {
					def dimlimit = params.dimlimit ? params.dimlimit : 280
					def img = ImageIO.read(inputStream)
	 
					def width = img?.width            
					def height = img?.height
	 
					def ratio = 1.0
					def newBytes
					
					if (img){
						
						if (width > dimlimit || height > dimlimit){
							// shrink by the smaller side so it can still be over the limit
							def dimtochange = width > height ? height : width
							ratio = dimlimit / dimtochange
							
							width = Math.floor(width * ratio).toInteger()
							height = Math.floor(height * ratio).toInteger()
						}
						
					  newBytes = doScale(img, width, height, ratio, fileext)
					 if (newBytes?.size() > 0){
						bytes = newBytes  
					 }
					}
				}
				catch(Exception e){
					logger.info(e.localizedMessage)   
					
				}
            }
			else {
                
			bytes = IOUtils.toByteArray(inputStream);			
			}
            */
            bytes = IOUtils.toByteArray(inputStream);
            outerMap.byteCount = bytes?.size()
 
            FileInfoBridge fib = fileInfoBridge
            FileInfo myImageFile = new FileInfo(filelabel: filelabel,
                                                filename: filename,
                                                filesize: bytes?.size(),
                                                description: description,
                                                tags: tag
                                                )
 
            myImageFile.contentType = contType
 
            FileUploadSession fus = new FileUploadSession();
            fus.files = [myImageFile]
 
            ExecutionResult fer = fileUploadSessionBridge.create(fus);
            myImageFile.sessionId = fer.succeeded.getAt(0)?.id
            
            ExecutionResult fileInfoResult = fib.create(myImageFile)
            
            if (fileInfoResult.successful) {
                outerMap.fileInfoSave = "File Info Saved"
                outerMap.sessionId = "File Upload SessionID: "+fer.succeeded.getAt(0)?.id
                outerMap.fileInfoId = "FileInfo ID: "+fileInfoResult?.succeeded.getAt(0)?.id
                ExecutionResult er = fib.saveOrUpdate(fileInfoResult.succeeded.getAt(0).id,new ByteArrayInputStream(bytes))
                def fileInfoId = fileInfoResult?.succeeded.getAt(0)?.id
                String url = "/services/v1/rest/Scripto/execute/vgs_DownloadFile?fileId=${fileInfoId}"
                if (er.successful) {
                    outerMap.url = url
                } else {
                    outerMap.save = "false"
                    logger.info(logFailure(er,outerMap))
                }
            } else {
                logger.info(logFailure(fileInfoResult, outerMap))
            }
 
        } else {
            outerMap.bytesAvail = "No bytes found to upload"
        }
 
	filesList << outerMap
 
    // return the JSONBuilder contents
    // we specify the content type, and any object as the return (even an outputstream!)
    return ["Content-Type": contentType,"Content":JSONArray.fromObject(filesList).toString(2)]
 
    // alternately you may just want to serial an Object as JSON:
    // return ["Content-Type": contentType,"Content":JSONArray.fromObject(invertedMessages).toString(2)]
 
} catch (Exception e) {
 
    // I knew you were exceptional!
    // we'll capture the output of the stack trace and return it in JSON
 
    json.Exception(
            description: "Execution Failed!!! An Exception was caught...",
            stack: ExceptionUtils.getFullStackTrace(e)
    )
 
    // return the output
    return ["Content-Type": contentType, "Content": json.toPrettyString()]
}
 
def doScale(image, width, height, ratio, fileext){
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    def bytes
    
    if (image){
     def scaledImg = new BufferedImage( width, height, BufferedImage.TYPE_INT_RGB )
       Graphics2D g = scaledImg.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.scale(ratio,ratio)
        g.drawImage(image, null, null);
        g.dispose();
        
       ImageIO.write( scaledImg, fileext, baos )
      baos.flush()   
      bytes = baos.toByteArray()
      baos.close()   
    }
    else { logger.info("scaling image is null")}
  return bytes     
}
/*
def doScale(image, width, height, ratio, fileext){
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    def bytes
    if (image){
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage after = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        def transform = new AffineTransform();
        transform.scale(width, height);
        AffineTransformOp scaleOp = 
           new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
        after = scaleOp.filter(image, after)
        ImageIO.write( op.filter(image,null), fileext, baos )
      baos.flush()   
      bytes = baos.toByteArray()
      baos.close()   
    }
    else { logger.info("scaling image is null")}
  return bytes     
}
 */
def deleteOldImage(url){
    def fileid = url.split(/\?fileId=/).getAt(1)
    return fileInfoBridge.delete(fileid)
}
 
 def sanitizeDescription(String description){
	return description.replaceAll(/\-/,"--").replaceAll(/(\s|_|,)/,"-")
 }
 
private void logFailure(ExecutionResult fileInfoResult, LinkedHashMap outerMap) {
    outerMap.message = fileInfoResult.failures.getAt(0)?.message
    outerMap.source = fileInfoResult.failures.getAt(0)?.sourceOfFailure
    outerMap.details = fileInfoResult.failures.getAt(0)?.details?.toString()
    outerMap.fileInfoSave = "false"
}
