# Deploying the backend to Oracle Cloud (Always Free)

Frontend goes to Vercel (separate, see main README). This covers the Spring
Boot backend on an Oracle Cloud "Always Free" AMD micro VM, fronted by
Caddy for automatic free HTTPS via nip.io (no domain required).

We use the AMD shape rather than the Ampere A1 (ARM) shape because A1
capacity is frequently unavailable ("out of host capacity") in most regions
-- the AMD micro shape has no such contention and is available immediately,
at the cost of a much smaller machine (1GB RAM). `setup-vm.sh` compensates
with a swap file and a memory-capped JVM; fine for early-stage/low-traffic
use, but worth migrating to a bigger instance (A1, if you can ever get one,
or a small paid shape) once real usage picks up.

## 1. Create the VM (OCI Console)

1. **Compute -> Instances -> Create Instance**.
2. Name it (e.g. `studyhub-backend`).
3. **Image and shape** -> Edit -> Image: **Canonical Ubuntu 22.04** (the
   default x86_64 build) -> Shape: **VM.Standard.E2.1.Micro** (Always Free,
   1/8 OCPU, 1GB RAM -- always available, no capacity waitlist).
4. **Networking**: use the default VCN/subnet, and tick **"Assign a public
   IPv4 address"**. For a stable HTTPS hostname later, reserve this as a
   **Reserved Public IP** (Networking -> IP Management) rather than an
   ephemeral one -- ephemeral IPs can change on stop/start and would break
   the nip.io hostname + certificate.
5. **Add SSH keys**: let OCI generate a key pair and download the private
   key, or paste your own public key. Keep the private key -- you'll need it
   to SSH in.
6. Create. This shape is Always Free with no capacity contention, so it
   should provision immediately.

## 2. Open the firewall (OCI Security List)

By default only port 22 is open. Go to your VM's **subnet -> Security List
-> Add Ingress Rules**, and add, each with Source CIDR `0.0.0.0/0`:
- TCP port 80
- TCP port 443

(Port 22 for SSH should already be open from the default list.)

This is a *cloud-level* firewall, separate from the OS firewall inside the
VM -- `setup-vm.sh` handles the OS-level `iptables` rules for you, but this
console step has to be done manually since it's outside the VM.

## 3. SSH in and run the setup script

```bash
ssh -i /path/to/your-private-key.key ubuntu@<VM_PUBLIC_IP>
curl -fsSL https://raw.githubusercontent.com/dummytemp87-code/Java_Pkrris/main/spring-backend/deploy/setup-vm.sh -o setup-vm.sh
chmod +x setup-vm.sh
./setup-vm.sh
```

First run will stop partway and tell you to fill in
`/etc/studyhub-backend.env` (DB credentials, JWT secret, YouTube/Gemini keys,
and `CORS_ALLOWED_ORIGINS` set to your Vercel URL). Edit it, then re-run
`./setup-vm.sh` -- it's idempotent.

## 4. Verify

```bash
curl https://<ip-with-dashes>.nip.io/actuator/health
# {"status":"UP"}
```

## 5. Point the frontend at it

In Vercel's project settings, set env var:
```
NEXT_PUBLIC_BACKEND_URL=https://<ip-with-dashes>.nip.io
```
and redeploy.

## Updating after future code changes

```bash
ssh -i /path/to/your-private-key.key ubuntu@<VM_PUBLIC_IP>
cd Java_Pkrris/spring-backend/deploy
./update.sh
```
