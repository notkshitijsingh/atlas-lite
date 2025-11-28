# AtlasDB-Lite Reference Manual

This document provides a complete reference for interacting with AtlasDB-Lite via the Secure Shell (CLI) and the HTTP Web API.

---

## Part 1: CLI Command Reference

The AtlasDB Shell (`atlas-secure>`) supports UNIX-style commands. Arguments are space-separated.

### 🛠️ Data Management

| Command | Syntax | Description |
| :--- | :--- | :--- |
| **add-node** | `add-node <id> <label> [k:v]...` | Creates a new node with optional properties.<br>**Example:** `add-node u1 User name:Alice role:Admin` |
| **update-node** | `update-node <id> <key> <val>` | Updates or adds a single property to an existing node.<br>**Example:** `update-node u1 status:Active` |
| **delete-node** | `delete-node <id>` | Deletes a node and **cascades** the deletion to all attached relationships.<br>**Example:** `delete-node u1` |
| **link** | `link <from> <to> <type>` | Creates a directional relationship between two existing nodes.<br>**Example:** `link u1 s1 MANAGES` |
| **nuke** | `nuke --confirm` | **DANGER:** Wipes the entire database memory and disk storage. Irreversible. |

### 🔍 Query & Search

| Command | Syntax | Description |
| :--- | :--- | :--- |
| **show** | `show` | Lists all nodes currently loaded in memory. |
| **query** | `query <id> <type>` | Traverses the graph from a start node to find targets connected by a specific relationship type.<br>**Example:** `query u1 MANAGES` |
| **search** | `search <text>` | Performs a **fuzzy search** across Node IDs, Labels, and Property Values.<br>**Example:** `search admin` |

### ⚙️ Administration & Tools

| Command | Syntax | Description |
| :--- | :--- | :--- |
| **stats** | `stats` | Displays node/edge counts, storage file size, and encryption method. |
| **backup** | `backup` | Creates a timestamped snapshot of the encrypted database file (e.g., `backup_20231128.enc`). |
| **export** | `export <filename.dot>` | Exports the current graph structure to a GraphViz `.dot` file for visualization. |
| **server** | `server <start|stop> [port]` | Controls the embedded Web API server.<br>**Default Port:** 8080. |

---

## Part 2: Web API Reference

When the server is running (`server start`), you can interact with the database via HTTP.

**Base URL:** `http://localhost:8080/api`
**Content-Type:** `application/json`

### 📡 System Endpoints

#### Check Status
Returns the server status.
* **GET** `/api/status`
* **Response:** `200 OK`
    ```json
    {
      "status": "online",
      "engine": "AtlasDB-Lite"
    }
    ```

---

### 📦 Node Operations

#### List All Nodes
Retrieve all nodes in the graph.
* **GET** `/api/nodes`
* **Response:** `200 OK`
    ```json
    [
      {
        "id": "u1",
        "label": "User",
        "properties": { "name": "Alice" }
      }
    ]
    ```

#### Create Node
Add a new node to the graph.
* **POST** `/api/node`
* **Body:**
    ```json
    {
      "id": "s-01",
      "label": "Server",
      "props": {
        "ip": "10.0.0.5",
        "os": "Linux"
      }
    }
    ```
* **Response:** `201 Created`

---

### 🔗 Relationship Operations

#### Create Link
Connect two nodes. Both nodes must exist before linking.
* **POST** `/api/link`
* **Body:**
    ```json
    {
      "from": "u1",
      "to": "s-01",
      "type": "OWNS"
    }
    ```
* **Response:** `201 Created`

---

### 🔎 Search

#### Fuzzy Search
Find nodes matching a query string.
* **GET** `/api/search?q=<query>`
* **Example:** `/api/search?q=Linux`
* **Response:** `200 OK` (Returns list of matching Nodes)