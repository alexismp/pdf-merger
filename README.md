# Your own PDF Merger (as a Service)

## Merge multiple PDF files into one
This small Java 11 SpringBoot app merges PDF files (think multiple page scans into one file) using a magic ghostscript command.

Don't judge the HTML.

In contrast with the online options already available to merge PDF into one, while very basic, this solution here can be used as a regular Spring Boot app, a standalone Docker image, or better yet, deployed to [Cloud Run](cloud.run). The application can also be improved in many ways, and mostly on the UI/UX front.

If you'd like to become a hero and make this available as a service for your friends and family or for your company then deploying this container image to Cloud Run (see 'Getting started' section below) is an interesting option. You should also be able to deploy this to [Cloud Run on GKE](https://cloud.google.com/run/docs/gke/setup) if you'd rather run on a different environment.

## A word on privacy

This app deletes all files after the merged PDF document has been generated.

## A word on concurrency

TODO

## Getting started

Build and create container image using [Jib](https://github.com/GoogleContainerTools/jib) :

`$ mvn compile jib:build -Dimage=<your image, eg. gcr.io/PROJECT-ID/pdfmerger>`

Run locally using Docker :

`docker run -p 8080:8080 -t gcr.io/PROJECT-ID/pdfmerger`

Deploy to Cloud Run using :

`$ gcloud run deploy --image gcr.io/PROJECT-ID/pdfmerger --platform managed`


## Resources
* [Magic ghostscript command](https://stackoverflow.com/questions/2507766/merge-convert-multiple-pdf-files-into-one-pdf)
* [Deploy to Cloud Run](https://cloud.google.com/run/docs/quickstarts/build-and-deploy)
* [Jib, Containerize your Java application](https://github.com/GoogleContainerTools/jib)


### Disclaimer

This list is not an official Google product. Links on this list also are not necessarily to official Google products.

### Contributing

If you have found or built something awesome that uses Google Cloud Platform, please follow the instructions in [CONTRIBUTING.md](CONTRIBUTING.md) to get it included here.