# Your own PDF Merger (as a Service)

## Merge multiple PDF files into one
This small Java 11 SpringBoot app merges PDF files (think multiple page scans into one file) using a simple Linux command.

Don't judge the HTML.

In contrast with the online options already available to merge PDF into one, while very basic, this solution here can be used as a regular Spring Boot app, a standalone Docker image, or better yet, deployed to [Cloud Run](cloud.run). The application can also be improved in many ways, and mostly on the UI/UX front (see the [Contributing](#a-word-on-contributing) section below). It shouldn't also not be too hard for folks to adapt the code to turn this into a service instead of a web app.

If you'd like to become a hero and make this available as a service for your friends and family or for your company then deploying this container image to Cloud Run (see the [Getting started](#getting-started) section below) is an interesting option. You should also be able to deploy this to [Cloud Run on GKE](https://cloud.google.com/run/docs/gke/setup) if you'd rather run on a different environment.

## A word on contributing

Yes, the UI is crap and the UX is just as bad.
Happy to accept contributions adding the ability to :
* manipulate an arbitrary number of files (selecting multiple files in a single input doesn't help with order). The backend is not written for a hard-coded number of files.
* re-order files before uploading them (in which case a single input for mutiple files could work)
* provide an upload tracker for large files
* preview files before they're uploaded
* client-side testing that the files actually contain PDF
* (more UI/UX improvements)

## A word on privacy

This app uses only the container's local filesystem and deletes all files after the merged PDF document has been generated.
I am not interested in developing features to store any data in file storage services of any kind.

## A word on concurrency

Concurrency can be hard. This implementation accounts for Cloud Run's built-in [concurrency](https://cloud.google.com/run/docs/about-concurrency) to make sure multiple users don't end up mixing their files (ugh!) since they share the same filesystem. I’m grouping files by user with a common prefix and preserving their order in a List.

You could use a simpler implementation and set the concurrency of Cloud Run to 1, essentially mimicking what FaaS products usually do, but this would likely increase the number of cold starts and thus worsen the user experience.

## Getting started

Build and create container image using [Jib](https://github.com/GoogleContainerTools/jib) :

`$ mvn compile jib:build -Dimage=<your image, eg. gcr.io/PROJECT-ID/pdfmerger>`

Run locally using Docker :

`docker run -p 8080:8080 -t gcr.io/PROJECT-ID/pdfmerger`

Deploy to Cloud Run using this command (or use the [console](https://console.cloud.google.com/run)) :

`$ gcloud run deploy --image gcr.io/PROJECT-ID/pdfmerger --platform managed --allow-unauthenticated --memory=1024Mi `

You can also deploy using this pre-built public image: `gcr.io/alexismp-pdfmerger/pdfmerger`

## Deploy with Cloud Run

[![Run on Google Cloud](https://deploy.cloud.run/button.svg)](https://deploy.cloud.run)

## Resources
* [Deploy to Cloud Run](https://cloud.google.com/run/docs/quickstarts/build-and-deploy)
* [Jib, Containerize your Java application](https://github.com/GoogleContainerTools/jib)

### Disclaimer

This list is not an official Google product. Links on this list also are not necessarily to official Google products.

### Contributing

If you have found or built something awesome that uses Google Cloud Platform, please follow the instructions in [CONTRIBUTING.md](CONTRIBUTING.md) to get it included here.