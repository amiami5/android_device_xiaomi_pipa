/*
 * Minimal stub for libmodemenhance_aidl_client.so required by libcameraimpl.so.
 * pipa is a Wi-Fi only tablet with no cellular modem, so IModemSmartEngine::asInterface
 * safely returns an empty android::sp (nullptr).
 */
void* _ZN3com8qualcomm3qti8internal9telephony12modemenhance3api17IModemSmartEngine11asInterfaceERKN7android2spINS7_7IBinderEEE(void) {
    return (void*)0;
}
