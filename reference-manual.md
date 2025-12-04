
# AtlasDB-Lite v3.4 Command Reference

---

## 🔐 Security Note
All data is stored in `atlas_db/` using **AES-256 encryption**.  
The key is stored in `atlas.key`.  
**Do not lose `atlas.key` or your data will be unrecoverable.**

---

## 🛠️ Data Management

| **Command**      | **Usage**                                      | **Description**                                                                 |
|-------------------|-----------------------------------------------|---------------------------------------------------------------------------------|
| `add-node`        | `add-node [id] <label> [key:val]...`         | Creates a new node. ID is optional.<br>• Explicit ID: `add-node u1 User name:Alice`<br>• Auto ID: `add-node User name:Bob` |
| `update-node`     | `update-node <search> <key> <val>`           | Updates/adds a property. Supports fuzzy search.<br>Ex: `update-node Alice role:Admin` |
| `delete-node`     | `delete-node <search>`                       | Deletes a node and all connected edges. Supports fuzzy search.                 |
| `link`            | `link <from> <to> <type>`                    | Connects two nodes. Supports interactive fuzzy search.<br>Ex: `link Alice Bob KNOWS` |
| `delete-link`     | `delete-link <from> <to> <type>`             | Removes a specific relationship.                                               |
| `update-link`     | `update-link <from> <to> <old> <new>`        | Renames a relationship type.<br>Ex: `update-link u1 s1 OWNS MANAGES`          |
| `nuke`            | `nuke --confirm`                             | **DANGER:** Wipes the entire database.                                         |

---

## 🔍 Query & Analytics

| **Command**      | **Usage**                                      | **Description**                                                                 |
|-------------------|-----------------------------------------------|---------------------------------------------------------------------------------|
| `select`          | `select <lbl> where <key> <op> <val>`        | AQL Engine: Runs SQL-like queries.<br>Ex: `select User where age > 18`        |
| `path`            | `path <from> <to>`                           | Finds the shortest path between two nodes.<br>Ex: `path Alice "Backup Server"`|
| `query`           | `query <id> <type>`                          | 1-Hop Traversal. Finds targets connected by specific relation.                 |
| `search`          | `search <text>`                              | Fuzzy search for nodes by ID, Label, or Property.                              |
| `index`           | `index <on|off>`                             | Toggles O(1) auto-indexing for faster lookups.                                 |
| `show`            | `show`                                       | Lists all nodes currently loaded in memory.                                    |

---

## ⚙️ Administration & Tools

| **Command**      | **Usage**                                      | **Description**                                                                 |
|-------------------|-----------------------------------------------|---------------------------------------------------------------------------------|
| `server`          | `server <start|stop> [port]`                 | Starts the Web Dashboard & API.<br>Visit `http://localhost:8080` for the visualizer. |
| `stats`           | `stats`                                       | Displays node counts, shard usage, and storage size.                           |
| `backup`          | `backup`                                      | Creates a timestamped snapshot of the encrypted shards.                        |
| `export`          | `export <file.dot>`                           | Exports graph to GraphViz DOT format.                                          |
| `exit`            | `exit`                                        | Saves all shards, encrypts data, and closes the shell.                         |

---

## 💡 Smart Features

- **Fuzzy Resolution:** Commands like `link`, `path`, and `update` allow you to type names (e.g., `"Alice"`) instead of IDs. If multiple matches are found, an interactive menu will appear.
- **Auto-ID:** If you omit the ID in `add-node`, a short UUID (8-chars) is automatically generated.

---
