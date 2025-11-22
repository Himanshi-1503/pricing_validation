# Render Performance & Troubleshooting Guide

## ⏱️ Why Postman Requests Are Slow

### Render Free Tier Behavior

**Render free tier apps automatically spin down after 15 minutes of inactivity.**

This means:
- ✅ **First request after spin-down**: Takes **~30 seconds** (app is waking up)
- ✅ **Subsequent requests**: Fast (normal speed, < 1 second)
- ⚠️ **After 15 min inactivity**: App spins down again

---

## 🚀 How to Speed Up Requests

### Option 1: Keep App Awake (Recommended)

**Use a simple health check every 10-14 minutes:**

1. **Set up a recurring request in Postman:**
   - Create a GET request: `https://your-app.onrender.com/api/pricing/`
   - Use Postman's **Collection Runner** or **Monitor** feature
   - Schedule it to run every 10 minutes

2. **Or use a free service:**
   - [UptimeRobot](https://uptimerobot.com) - Free monitoring (checks every 5 minutes)
   - [Cron-job.org](https://cron-job.org) - Free cron jobs
   - Set up a simple GET request to your API every 10 minutes

### Option 2: Wait for First Request

- **First request**: Wait ~30 seconds (app is waking up)
- **All other requests**: Will be fast immediately after

### Option 3: Upgrade to Paid Tier

- **Starter Plan ($7/month)**: App stays awake 24/7
- **No spin-down delays**

---

## 🔍 Check If App Is Running

### Method 1: Check Render Dashboard

1. Go to [render.com](https://render.com) → Dashboard
2. Click your **Web Service**
3. Check **Status**:
   - 🟢 **Live** = App is running
   - 🟡 **Building** = App is deploying
   - 🔴 **Stopped** = App is stopped (needs restart)

### Method 2: Check Logs

1. In Render Dashboard → Your Service → **Logs** tab
2. Look for:
   - ✅ `Started PricingValidationApplication` = App is running
   - ⏳ `Waiting for app to start` = App is waking up
   - ❌ Error messages = Something is wrong

### Method 3: Test in Browser

Open in browser:
```
https://your-app.onrender.com/api/pricing/
```

- ✅ **Fast response** = App is awake
- ⏳ **Takes 30 seconds** = App is waking up
- ❌ **Error/timeout** = Check logs

---

## ⚡ Quick Performance Tips

### 1. **Batch Your Requests**
- Do all testing in one session (within 15 minutes)
- App stays awake during active use

### 2. **Use Postman Collection Runner**
- Run all requests in sequence
- First request wakes app, rest are fast

### 3. **Check App Status First**
- Always start with: `GET /api/pricing/`
- If it responds quickly, app is awake
- If slow, wait 30 seconds then continue

### 4. **Monitor Response Times**
- First request: ~30 seconds (normal for spin-down)
- Subsequent requests: < 1 second (normal)

---

## 🐛 Common Issues & Solutions

### Issue: "Request timeout" or "Connection timeout"

**Cause**: App is spinning up (takes ~30 seconds)

**Solution**:
1. Wait 30-45 seconds
2. Try the request again
3. If still timing out, check Render logs

---

### Issue: "502 Bad Gateway"

**Cause**: App crashed or failed to start

**Solution**:
1. Check Render **Logs** tab
2. Look for error messages
3. Common causes:
   - Missing environment variables
   - Port configuration issue
   - Application startup error
4. Try **Manual Deploy** → **Clear build cache & deploy**

---

### Issue: "503 Service Unavailable"

**Cause**: App is still building or starting

**Solution**:
1. Check Render Dashboard → **Events** tab
2. Wait for build to complete
3. Check **Logs** for startup messages

---

### Issue: "404 Not Found"

**Cause**: Wrong URL or endpoint

**Solution**:
1. Verify URL: `https://your-app.onrender.com/api/pricing/`
2. Check endpoint path (should start with `/api/pricing/`)
3. Test in browser first

---

## 📊 Expected Response Times

| Scenario | Expected Time |
|----------|---------------|
| App awake, first request | < 1 second |
| App spinning up, first request | ~30 seconds |
| App awake, subsequent requests | < 1 second |
| App building/deploying | 2-5 minutes |

---

## ✅ Quick Health Check

**Test this first to verify app is working:**

```http
GET https://your-app.onrender.com/api/pricing/
```

**Expected Response:**
```json
{
  "name": "Pricing Data Validation & Reporting Utility",
  "version": "1.0.0",
  "description": "API for validating and managing CME pricing data",
  ...
}
```

**If this works**: App is running! ✅  
**If this is slow**: App is waking up, wait 30 seconds ⏳  
**If this fails**: Check Render logs ❌

---

## 💡 Pro Tips

1. **Always start with health check** (`GET /api/pricing/`)
2. **Keep testing session active** (within 15 minutes)
3. **Use Postman Collection Runner** for batch testing
4. **Check Render logs** if something doesn't work
5. **First request is always slow** - this is normal!

---

## 🎯 Summary

- ⏳ **30-second delay is NORMAL** for first request after spin-down
- ✅ **Subsequent requests are FAST** (< 1 second)
- 🔄 **App spins down after 15 min** of inactivity
- 💡 **Keep app awake** with periodic health checks

**This is expected behavior for Render free tier!** 🚀

