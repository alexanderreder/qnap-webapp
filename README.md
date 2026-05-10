# QNAP-WebApp Umbrella Application

This project combines the `car-statistics` and `teaching` Spring Boot applications into a single executable JAR. This approach significantly reduces the memory footprint on resource-constrained devices like a QNAP NAS by running all applications within a single Java Virtual Machine (JVM).

The umbrella application starts three separate Spring Application contexts on different ports:
- **Landing Page**: `http://<nas-ip>:8080`
- **Car Statistics**: `http://<nas-ip>:8081`
- **Teaching App**: `http://<nas-ip>:8082`

## Deployment on QNAP TS-231P (ARM32)

Deploying to older ARM32 NAS devices like the QNAP TS-231P requires bundling a specific Java Runtime Environment (JRE) because the native OS (QTS) uses an older C Library (`glibc` 2.21 or older). Standard Java 17/21 builds require newer `glibc` versions and will fail with `dl failure` errors.

To run this application on a QNAP TS-231P, follow these steps to create a standalone bundle using the **BellSoft Liberica Java 17 JRE** (which is compiled for compatibility with older `glibc` libraries).

### 1. Build the Bundle
Run the following commands on your development machine (Linux/macOS):

```bash
# 1. Ensure pom.xml is set to Java 17: <java.version>17</java.version>
# 2. Build the JAR
cd carstatistics
mvn clean package -DskipTests

# 3. Create bundle directory
cd ..
mkdir -p carstatistics-bundle/jre

# 4. Download BellSoft Liberica Java 17 for ARM32 (Hard Float)
wget https://download.bell-sw.com/java/17.0.14+10/bellsoft-jre17.0.14+10-linux-arm32-vfp-hflt.tar.gz -O /tmp/jre-arm32.tar.gz

# 5. Extract JRE and copy the JAR
tar -xzf /tmp/jre-arm32.tar.gz -C carstatistics-bundle/jre --strip-components=1
cp carstatistics/target/carstatistics-1.0.0-SNAPSHOT.jar carstatistics-bundle/app.jar
```

### 2. Create the Startup Script (`run.sh`)
Inside the `carstatistics-bundle` folder, create a file named `run.sh`:

```bash
#!/bin/sh
DIR="$(cd "$(dirname "$0")" && pwd)"

# Configuration
APP_PORT="8181"
DB_HOST="venus"
DB_PORT="3306"
DB_NAME="cardb"
DB_USER="caradmin"
DB_PASS="caradmin"

DB_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"

$DIR/jre/bin/java -jar $DIR/app.jar \
  --server.port="${APP_PORT}" \
  --spring.datasource.url="${DB_URL}" \
  --spring.datasource.username="${DB_USER}" \
  --spring.datasource.password="${DB_PASS}" \
  --spring.profiles.active=mysql
```
Make it executable and compress the bundle:
```bash
chmod +x carstatistics-bundle/run.sh
tar -czvf carstatistics-qnap.tar.gz -C carstatistics-bundle .
```

### 3. Transfer and Extract on QNAP
1. Transfer `carstatistics-qnap.tar.gz` to your NAS (e.g., via QNAP File Station or `scp`).
2. SSH into your QNAP and extract it:
   ```bash
   mkdir -p /share/Public/carstatistics
   tar -xzf carstatistics-qnap.tar.gz -C /share/Public/carstatistics
   ```

### 4. Configure Autostart (`autorun.sh`)
To start the application automatically when the NAS reboots:

1. In the QTS Web UI, go to **Control Panel** -> **System** -> **Hardware** and enable **"Run user defined startup processes (autorun.sh)"**.
2. SSH into the QNAP and mount the configuration partition:
   ```bash
   ubiattach -m 6 -d 2
   mkdir -p /tmp/config
   mount -t ubifs ubi2:config /tmp/config
   ```
3. Edit the autorun file: `vi /tmp/config/autorun.sh`
4. Add the following (adjusting the path to where you extracted the bundle):
   ```bash
   #!/bin/sh
   # Wait 60s for DB and network to be ready
   sleep 60
   cd /share/Public/carstatistics
   sh ./run.sh > ./application.log 2>&1 &
   ```
5. Save, make executable, and unmount:
   ```bash
   chmod a+x /tmp/config/autorun.sh
   umount /tmp/config
   ubidetach -m 6
   ```
