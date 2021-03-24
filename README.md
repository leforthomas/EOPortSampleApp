This is a sample application project showing how to implement a basic EO PORT service.

The sample is available in Java and Python.

The sample service implements the workflow with the production manager, you will need to add the processing.
The steps taken are
- receives the notification from the production manager
- downloads the file from the input service
- (processing - replace with your own code) extracts some metadata and generates a shapefile
- saves the generated file to the S3 repository used for file exchanges and notifies the poduct
- notifies the production manager of the new product availability
