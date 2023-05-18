// metadata.js

export async function getMetadata(url) {
    // Make a request to the URL
    const response = await fetch(url, { mode: 'no-cors' });
    const html = await response.text();
  
    // Parse the HTML using the DOMParser API
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');
  
    // Extract the title and description from the HTML
    const title = doc.querySelector('title').innerHTML;
    const description = doc.querySelector('meta[name="description"]')?.getAttribute('content') || '';
  
    // Return an object containing the URL, title, and description
    return { url, title, description };
  }
