package febit.libci

class Notice extends Exception {

    Notice(String message) {
        super(message, null, true, false)
    }
}
