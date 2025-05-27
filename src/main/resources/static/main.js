document.addEventListener('DOMContentLoaded', () => {
    // Configure PDF.js worker source
    // Ensure pdf.js is loaded before this script, or this will fail.
    // If pdf.js is loaded with defer, main.js also with defer should be fine.
    if (typeof pdfjsLib === 'undefined') {
        console.error('PDF.js library not loaded. Ensure pdf.js is included before main.js');
        return;
    }
    pdfjsLib.GlobalWorkerOptions.workerSrc = './lib/pdfjs/pdf.worker.js';

    const fileInputsContainer = document.getElementById('fileInputsContainer');
    const addFileButton = document.getElementById('addFileButton');

    // Define handleFileSelect function once
    const handleFileSelect = (event) => {
        const file = event.target.files[0];
        const rowDiv = event.target.closest('.fileInputRow');
        if (!rowDiv) return;
        const canvas = rowDiv.querySelector('.thumbnailCanvas');
        if (!canvas) return;

        const context = canvas.getContext('2d');
        context.clearRect(0, 0, canvas.width, canvas.height);

        if (!file) {
            return; // No file selected
        }

        if (file.type !== 'application/pdf') {
            // context.fillText('Not a PDF', 10, 20); // Optional: message, needs font settings
            console.warn('Selected file is not a PDF.');
            return;
        }

        const fileReader = new FileReader();
        fileReader.onload = function() {
            const typedArray = new Uint8Array(this.result);

            pdfjsLib.getDocument({ data: typedArray }).promise.then(pdfDoc_ => {
                // console.log("PDF loaded");
                pdfDoc_.getPage(1).then(page => {
                    // console.log("Page 1 loaded");
                    
                    let viewport = page.getViewport({ scale: 1 });
                    let scale = Math.min(canvas.width / viewport.width, canvas.height / viewport.height);
                    let scaledViewport = page.getViewport({ scale: scale });

                    const renderContext = {
                        canvasContext: context,
                        viewport: scaledViewport
                    };
                    page.render(renderContext).promise.then(() => {
                        // console.log('Page rendered');
                    }).catch(renderErr => {
                        console.error('Error rendering page: ' + renderErr);
                        // context.fillText('Render err', 10, 20);
                    });
                }).catch(pageErr => {
                    console.error('Error getting page 1: ' + pageErr);
                    // context.fillText('Page err', 10, 20);
                });
            }).catch(docErr => {
                console.error('Error loading PDF document: ' + docErr);
                // context.fillText('Load err', 10, 20);
            });
        };
        fileReader.readAsArrayBuffer(file);
    };

    const createFileInputRow = () => {
        const rowDiv = document.createElement('div');
        rowDiv.classList.add('fileInputRow');

        const fileInput = document.createElement('input');
        fileInput.type = 'file';
        fileInput.name = 'files'; // Ensure the name matches what the backend expects
        fileInput.classList.add('pdfFile');
        // fileInput.accept = '.pdf'; // Optional: client-side hint for file type

        const thumbnailCanvas = document.createElement('canvas');
        thumbnailCanvas.classList.add('thumbnailCanvas');
        thumbnailCanvas.width = 50; // Example size
        thumbnailCanvas.height = 70; // Example size

        const removeButton = document.createElement('button');
        removeButton.type = 'button';
        removeButton.classList.add('removeFileButton');
        removeButton.textContent = 'Remove';

        removeButton.addEventListener('click', () => {
            // Only allow removal if there's more than one file input row
            if (document.querySelectorAll('#fileInputsContainer .fileInputRow').length > 1) {
                rowDiv.remove();
            } else {
                alert('At least one file input must remain.');
            }
        });

        rowDiv.appendChild(fileInput);
        rowDiv.appendChild(thumbnailCanvas);
        rowDiv.appendChild(removeButton);

        // Add event listener for thumbnail generation
        fileInput.addEventListener('change', handleFileSelect);

        return rowDiv;
    };

    if (addFileButton) { // Ensure button exists before adding listener
        addFileButton.addEventListener('click', () => {
            const newRow = createFileInputRow();
            if (fileInputsContainer) { // Ensure container exists
                fileInputsContainer.appendChild(newRow);
            }
        });
    }

    // Add remove buttons, canvas, and event listeners to the initial, hardcoded rows
    if (fileInputsContainer) { // Ensure container exists
        const initialRows = fileInputsContainer.querySelectorAll('.fileInputRow');
        initialRows.forEach((row) => { 
            const fileInput = row.querySelector('.pdfFile');
            if (fileInput && !fileInput.dataset.listenerAttached) { // Check if listener already attached
                fileInput.addEventListener('change', handleFileSelect);
                fileInput.dataset.listenerAttached = 'true'; // Mark as listener attached
            }
            
            // Check if the row already has a canvas and remove button
            if (!row.querySelector('.thumbnailCanvas')) {
                const thumbnailCanvas = document.createElement('canvas');
                thumbnailCanvas.classList.add('thumbnailCanvas');
                thumbnailCanvas.width = 50;
                thumbnailCanvas.height = 70;
                row.appendChild(thumbnailCanvas); // Append canvas if not present
            }

            if (!row.querySelector('.removeFileButton')) {
                const removeButton = document.createElement('button');
                removeButton.type = 'button';
                removeButton.classList.add('removeFileButton');
                removeButton.textContent = 'Remove';

                removeButton.addEventListener('click', () => {
                    if (document.querySelectorAll('#fileInputsContainer .fileInputRow').length > 1) {
                        row.remove();
                    } else {
                        alert('At least one file input must remain.');
                    }
                });
                row.appendChild(removeButton); // Append remove button if not present
            }
        });
    }
});
            thumbnailCanvas.classList.add('thumbnailCanvas');
            thumbnailCanvas.width = 50;
            thumbnailCanvas.height = 70;

            const removeButton = document.createElement('button');
            removeButton.type = 'button';
            removeButton.classList.add('removeFileButton');
            removeButton.textContent = 'Remove';

            removeButton.addEventListener('click', () => {
                if (document.querySelectorAll('#fileInputsContainer .fileInputRow').length > 1) {
                    row.remove();
                } else {
                    alert('At least one file input must remain.');
                }
            });
            
            row.appendChild(thumbnailCanvas);
            row.appendChild(removeButton);
        });
    }
});
