 <style>
    html {
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
      line-height: 1.6;
      background-color: #f4f4f9;
      color: #333;
      padding: 20px;
    }

    h1 {
      color: #444;
      font-size: 2.5rem;
      text-align: center;
      margin-bottom: 1rem;
    }

    h2, h3, h4 {
      color: #666;
      margin: 0.5rem 0 0.5rem 0;
    }

    input,
    button {
      font-size: inherit;
      border: 1px solid #ddd;
      padding: 10px;
      border-radius: 4px;
      margin: 5px 0;
    }

    input[type=text] {
      min-width: 15rem;
    }

    button {
      background-color: #007bff;
      color: #fff;
      cursor: pointer;
      transition: background-color 0.3s;
    }

    button:hover {
      background-color: #0056b3;
    }

    #app {
      max-width: 1200px;
      margin: 0 auto;
      padding: 20px;
      background-color: #fff;
      border-radius: 8px;
      box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
    }

    table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 20px;
    }

    table th,
    table td {
      padding: 12px;
      border: 1px solid #ddd;
      text-align: left;
    }

    table th {
      background-color: #f4f4f4;
    }

    table tr:nth-child(even) {
      background-color: #f9f9f9;
    }

    a {
      color: #007bff;
      text-decoration: none;
    }

    a:hover {
      text-decoration: underline;
    }
  </style>

<h2>Resource Links</h2>

<table class="table-style">
    <thead>
        <tr>
            <th>Path</th>
            <th>Tag</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td><a href="/">/</a></td>
            <td></td>
            <td>Root</td>
        </tr>
        <tr>
            <td><a href="/inventory">/inventory</a></td>
            <td>JS-UI</td>
            <td>Inventory-Module</td>
        </tr>
        <tr>
            <td><a href="/inventory/models">/inventory/models</a></td>
            <td>API / GET</td>
            <td>Models-Endpoint</td>
        </tr>
        <tr>
            <td><a href="/inventory/js/main.js">/inventory/js/*</a></td>
            <td>PUBLIC</td>
            <td>JavaScript files</td>
        </tr>
        <tr>
            <td><a href="/inventory/assets/zhdk-logo.svg">/inventory/assets/*</a></td>
            <td>PUBLIC</td>
            <td>Asset files</td>
        </tr>
        <tr>
            <td><a href="/inventory/api-docs/index.html">/inventory/api-docs/index.html</a></td>
            <td>SWAGGER</td>
            <td>References to resources/public/*</td>
        </tr>
        <tr>
            <td><a href="/inventory/api-docs/">/inventory/api-docs/*</a></td>
            <td>SWAGGER</td>
            <td>Swagger-UI-Resources (Main)</td>
        </tr>
    </tbody>
</table>