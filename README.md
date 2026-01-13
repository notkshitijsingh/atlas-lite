# AtlasDB-Lite v4.0

## 📝 Proposed Changes
This PR implements **Interactive Node Management** directly within the Web 4.0 Dashboard.
- **Frontend Changes:**
  - Added a **Tools Panel** for creating Nodes and Links.
  - Implemented **Edit Mode** for real-time property updates.
  - Added **Context Menus (Right-Click)** for quick actions (Copy ID, Link, Delete).
  - Enhanced visual feedback for selection and clustering.
- **Backend Changes:**
  - `POST /api/node`: Create new nodes with custom properties.
  - `PUT /api/node`: Update existing node properties.
  - `DELETE /api/node/{id}`: Remove nodes from the graph.
  - `POST /api/link`: Create directed edges between nodes.

AtlasDB-Lite is a **scalable, serverless, and encrypted Knowledge Graph engine** written in pure Java.
It is designed for **embedded environments** where you need Graph capabilities (Relationships, Pathfinding) without the overhead of Docker containers or heavy database servers.

---

## 🚀 Key Features

- **Sharded Architecture:**
  Data is split across **16 encrypted buckets (Shards)**, allowing databases larger than available RAM via LRU caching.

- **Smart CLI:**
  UNIX-style shell with fuzzy search, interactive resolution, and auto-ID generation.

- **AQL (Atlas Query Language):**
  SQL-like filtering (e.g., `select User where age > 21`).

- **Interactive Web Dashboard:**
  Built-in web server provides an interactive graph visualization with **Node & Link Management**.

- **Secure:**
  All data at rest is encrypted with **AES-256**.

- **Crash Safe:**
  Atomic writes ensure no data corruption on power loss.

---

## 📦 Getting Started

### **Prerequisites**
- Java JDK 17 or higher
- Maven 3.6+

### **Installation**
```bash
# Clone & Build
git clone https://github.com/notkshitijsingh/atlasdb-lite.git
cd atlasdb-lite
mvn clean compile

# Launch the Shell
mvn exec:java
```
---
## 💻 Usage Examples
### 1. Creating Data (Smart Syntax)
You can provide an explicit ID or let Atlas generate one.
```bash
# Explicit ID
atlas-sharded> add-node u1 User name:Alice role:Admin

# Auto-Generated ID
atlas-sharded> add-node Server ip:10.0.0.1 os:Linux
```

### 2. Linking & Pathfinding
Connect nodes and find connections using natural names.
```bash

# Connect Alice to the Server
atlas-sharded> link Alice Server MANAGES

# Find how Alice reaches the backup system
atlas-sharded> path Alice "Backup Disk"
```

### 3. Advanced Querying (AQL)
Filter data using Logical Operators
```bash
atlas-sharded> select User where role = Admin
atlas-sharded> select Server where ip contains 192.168
```

### 4. Visual Dashboard
Visualize your graph in the browser.
```bash

# Start the server
atlas-sharded> server start 8080

# Open in browser
http://localhost:8080
```

**Web Interface Features:**
- **Tools Panel:** Create Nodes and Links (Types & Properties).
- **Direct Manipulation:** Select nodes to **Edit Properties**, **Delete**, or **Link** them visually.
- **Right-Click Menu:** Quick access to ID copying and linking.

### 5. Importing Data from CSV
You can bulk import nodes and links from CSV files.
```bash
# Import nodes
atlas-sharded> import csv/kanto_nodes.csv --type=node

# Import links
atlas-sharded> import csv/kanto_links.csv --type=link
```
The `csv` directory contains an example dataset based on the Kanto region from Pokémon.

---
## 🔐 Security

On first run, AtlasDB-Lite generates atlas.key.

This key is required to decrypt the atlas_db/ folder.

**Do not lose this key.**

---
<p align='center'>Made with ❤️ by <a href='https://github.com/notkshitijsingh/'>notkshitijsingh</a></p>