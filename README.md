# AtlasDB-Lite

**AtlasDB-Lite** is a lightweight, serverless, and encrypted Knowledge Graph engine written in pure Java.

Designed for restricted environments where deploying full-scale database servers (like Neo4j or SQL) is not feasible, AtlasDB-Lite runs entirely in user-space. It manages complex node-relationship data in memory while ensuring data at rest is secured with military-grade AES-256 encryption.

## 🚀 Key Features

* **Zero-Install Architecture:** Runs on any machine with a JVM. No Docker, Admin rights, or background services required.
* **Secure by Default:** All data is persisted to disk (`atlas_data.enc`) using **AES-256 encryption**.
* **Interactive Shell:** Includes a robust UNIX-style CLI (`AtlasShell`) for managing data.
* **Fuzzy Search:** Built-in search engine to find nodes by property values.
* **GraphViz Export:** Native support for exporting graph structures to `.dot` format for visualization.
* **Disaster Recovery:** Hot-backup command creates timestamped snapshots of your encrypted database.

## 🛠️ Technical Architecture

AtlasDB-Lite uses a manual Maven directory structure to maintain a minimal footprint.

* **Engine:** In-Memory Graph (Adjacency List + HashMap Indexing).
* **Persistence:** JSON serialization via Google Gson, wrapped in a custom `CryptoManager` layer.
* **Security:** `javax.crypto` (AES/ECB/PKCS5Padding).
* **Interface:** Command Pattern based REPL (Read-Eval-Print Loop).

## 📦 Getting Started

### Prerequisites
* Java JDK 17 or higher
* Maven 3.6+

### Installation

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/kshitijsinghcts/atlas-lite.git
    cd atlasdb-lite
    ```

2.  **Build the Project:**
    ```bash
    mvn clean compile
    ```

3.  **Launch the Secure Shell:**
    ```bash
    mvn exec:java
    ```

## 💻 Usage Guide

Once inside the `atlas-secure>` shell, you can interact with your graph immediately.

### 1. Creating Data
Create entities (Nodes) with dynamic properties.
```bash
atlas-secure> add-node u-101 User name:Alice role:DevOps location:NY
atlas-secure> add-node s-500 Server ip:192.168.1.5 os:Ubuntu
```
### 2. Linking Entities
Define relationships between nodes.

```Bash
atlas-secure> link u-101 s-500 MANAGES
atlas-secure> link s-500 u-101 ALERTS
```
### 3. Querying & Search
Traverse the graph or search text.
```Bash

# Find what Alice manages
atlas-secure> query u-101 MANAGES

# Find any node containing "Ubuntu"
atlas-secure> search Ubuntu
```

### 4. Administration
Manage the health and security of your data.

```Bash

# View database stats and encryption status
atlas-secure> stats

# Create a secure snapshot
atlas-secure> backup

# Export for visualization (Paste content into WebGraphviz)
atlas-secure> export graph_visual.dot
```
For a full list of commands, type help inside the shell or refer to Command Manual.

## 🔐 Security Information
Upon the first run, AtlasDB-Lite generates a unique key file: atlas.key.

**⚠️ CRITICAL WARNING:** This key is required to decrypt your atlas_data.enc file.

- Do not lose this key. If lost, your data is unrecoverable.

- Do not share this key. Anyone with the key and the data file can decrypt your graph.

## 🤝 Contributing
1. Fork the Project

2. Create your Feature Branch (git checkout -b feature/AmazingFeature)

3. Commit your Changes (git commit -m 'Add some AmazingFeature')

4. Push to the Branch (git push origin feature/AmazingFeature)

5. Open a Pull Request
---
<p align='center'>Made with ❤️ by <a href='https://www.github.com/notkshitijsingh'>notkshitijsingh</a></p>
