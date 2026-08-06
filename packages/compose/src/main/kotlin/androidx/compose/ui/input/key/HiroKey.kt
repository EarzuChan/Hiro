@file:android.annotation.SuppressLint("InlinedApi")

package androidx.compose.ui.input.key

import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION_MIX_DOWN
import android.view.KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION_MIX_UP
import androidx.compose.ui.input.key.Key.Companion.Number

@JvmInline
value class Key(val keyCode: Long) {
    companion object {
        
        val Unknown = Key(KeyEvent.KEYCODE_UNKNOWN)

        
        val SoftLeft = Key(KeyEvent.KEYCODE_SOFT_LEFT)

        
        val SoftRight = Key(KeyEvent.KEYCODE_SOFT_RIGHT)

        
        @Deprecated(
            "`Key.Home` is never delivered to applications. For the keyboard \"Home\" key " +
                "use `Key.MoveHome`. For the system \"Home\" key (unlikely to be needed), use " +
                "`Key.SystemHome`",
            level = DeprecationLevel.ERROR,
        )
        val Home = Key(KeyEvent.KEYCODE_HOME)

        
        val SystemHome = Key(KeyEvent.KEYCODE_HOME)

        
        val Back = Key(KeyEvent.KEYCODE_BACK)

        
        val Help = Key(KeyEvent.KEYCODE_HELP)

        
        val NavigatePrevious = Key(KeyEvent.KEYCODE_NAVIGATE_PREVIOUS)

        
        val NavigateNext = Key(KeyEvent.KEYCODE_NAVIGATE_NEXT)

        
        val NavigateIn = Key(KeyEvent.KEYCODE_NAVIGATE_IN)

        
        val NavigateOut = Key(KeyEvent.KEYCODE_NAVIGATE_OUT)

        
        val SystemNavigationUp = Key(KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP)

        
        val SystemNavigationDown = Key(KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN)

        
        val SystemNavigationLeft = Key(KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT)

        
        val SystemNavigationRight = Key(KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT)

        
        val Call = Key(KeyEvent.KEYCODE_CALL)

        
        val EndCall = Key(KeyEvent.KEYCODE_ENDCALL)

        
        val DirectionUp = Key(KeyEvent.KEYCODE_DPAD_UP)

        
        val DirectionDown = Key(KeyEvent.KEYCODE_DPAD_DOWN)

        
        val DirectionLeft = Key(KeyEvent.KEYCODE_DPAD_LEFT)

        
        val DirectionRight = Key(KeyEvent.KEYCODE_DPAD_RIGHT)

        
        val DirectionCenter = Key(KeyEvent.KEYCODE_DPAD_CENTER)

        
        val DirectionUpLeft = Key(KeyEvent.KEYCODE_DPAD_UP_LEFT)

        
        val DirectionDownLeft = Key(KeyEvent.KEYCODE_DPAD_DOWN_LEFT)

        
        val DirectionUpRight = Key(KeyEvent.KEYCODE_DPAD_UP_RIGHT)

        
        val DirectionDownRight = Key(KeyEvent.KEYCODE_DPAD_DOWN_RIGHT)

        
        val VolumeUp = Key(KeyEvent.KEYCODE_VOLUME_UP)

        
        val VolumeDown = Key(KeyEvent.KEYCODE_VOLUME_DOWN)

        
        val Power = Key(KeyEvent.KEYCODE_POWER)

        
        val Camera = Key(KeyEvent.KEYCODE_CAMERA)

        
        val Clear = Key(KeyEvent.KEYCODE_CLEAR)

        
        val Zero = Key(KeyEvent.KEYCODE_0)

        
        val One = Key(KeyEvent.KEYCODE_1)

        
        val Two = Key(KeyEvent.KEYCODE_2)

        
        val Three = Key(KeyEvent.KEYCODE_3)

        
        val Four = Key(KeyEvent.KEYCODE_4)

        
        val Five = Key(KeyEvent.KEYCODE_5)

        
        val Six = Key(KeyEvent.KEYCODE_6)

        
        val Seven = Key(KeyEvent.KEYCODE_7)

        
        val Eight = Key(KeyEvent.KEYCODE_8)

        
        val Nine = Key(KeyEvent.KEYCODE_9)

        
        val Plus = Key(KeyEvent.KEYCODE_PLUS)

        
        val Minus = Key(KeyEvent.KEYCODE_MINUS)

        
        val Multiply = Key(KeyEvent.KEYCODE_STAR)

        
        val Equals = Key(KeyEvent.KEYCODE_EQUALS)

        
        val Pound = Key(KeyEvent.KEYCODE_POUND)

        
        val A = Key(KeyEvent.KEYCODE_A)

        
        val B = Key(KeyEvent.KEYCODE_B)

        
        val C = Key(KeyEvent.KEYCODE_C)

        
        val D = Key(KeyEvent.KEYCODE_D)

        
        val E = Key(KeyEvent.KEYCODE_E)

        
        val F = Key(KeyEvent.KEYCODE_F)

        
        val G = Key(KeyEvent.KEYCODE_G)

        
        val H = Key(KeyEvent.KEYCODE_H)

        
        val I = Key(KeyEvent.KEYCODE_I)

        
        val J = Key(KeyEvent.KEYCODE_J)

        
        val K = Key(KeyEvent.KEYCODE_K)

        
        val L = Key(KeyEvent.KEYCODE_L)

        
        val M = Key(KeyEvent.KEYCODE_M)

        
        val N = Key(KeyEvent.KEYCODE_N)

        
        val O = Key(KeyEvent.KEYCODE_O)

        
        val P = Key(KeyEvent.KEYCODE_P)

        
        val Q = Key(KeyEvent.KEYCODE_Q)

        
        val R = Key(KeyEvent.KEYCODE_R)

        
        val S = Key(KeyEvent.KEYCODE_S)

        
        val T = Key(KeyEvent.KEYCODE_T)

        
        val U = Key(KeyEvent.KEYCODE_U)

        
        val V = Key(KeyEvent.KEYCODE_V)

        
        val W = Key(KeyEvent.KEYCODE_W)

        
        val X = Key(KeyEvent.KEYCODE_X)

        
        val Y = Key(KeyEvent.KEYCODE_Y)

        
        val Z = Key(KeyEvent.KEYCODE_Z)

        
        val Comma = Key(KeyEvent.KEYCODE_COMMA)

        
        val Period = Key(KeyEvent.KEYCODE_PERIOD)

        
        val AltLeft = Key(KeyEvent.KEYCODE_ALT_LEFT)

        
        val AltRight = Key(KeyEvent.KEYCODE_ALT_RIGHT)

        
        val ShiftLeft = Key(KeyEvent.KEYCODE_SHIFT_LEFT)

        
        val ShiftRight = Key(KeyEvent.KEYCODE_SHIFT_RIGHT)

        
        val Tab = Key(KeyEvent.KEYCODE_TAB)

        
        val Spacebar = Key(KeyEvent.KEYCODE_SPACE)

        
        val Symbol = Key(KeyEvent.KEYCODE_SYM)

        
        val Browser = Key(KeyEvent.KEYCODE_EXPLORER)

        
        val Envelope = Key(KeyEvent.KEYCODE_ENVELOPE)

        
        val Enter = Key(KeyEvent.KEYCODE_ENTER)

        
        val Backspace = Key(KeyEvent.KEYCODE_DEL)

        
        val Delete = Key(KeyEvent.KEYCODE_FORWARD_DEL)

        
        val Escape = Key(KeyEvent.KEYCODE_ESCAPE)

        
        val CtrlLeft = Key(KeyEvent.KEYCODE_CTRL_LEFT)

        
        val CtrlRight = Key(KeyEvent.KEYCODE_CTRL_RIGHT)

        
        val CapsLock = Key(KeyEvent.KEYCODE_CAPS_LOCK)

        
        val ScrollLock = Key(KeyEvent.KEYCODE_SCROLL_LOCK)

        
        val MetaLeft = Key(KeyEvent.KEYCODE_META_LEFT)

        
        val MetaRight = Key(KeyEvent.KEYCODE_META_RIGHT)

        
        val Function = Key(KeyEvent.KEYCODE_FUNCTION)

        
        val PrintScreen = Key(KeyEvent.KEYCODE_SYSRQ)

        
        val Break = Key(KeyEvent.KEYCODE_BREAK)

        
        val MoveHome = Key(KeyEvent.KEYCODE_MOVE_HOME)

        
        val MoveEnd = Key(KeyEvent.KEYCODE_MOVE_END)

        
        val Insert = Key(KeyEvent.KEYCODE_INSERT)

        
        val Cut = Key(KeyEvent.KEYCODE_CUT)

        
        val Copy = Key(KeyEvent.KEYCODE_COPY)

        
        val Paste = Key(KeyEvent.KEYCODE_PASTE)

        
        val Grave = Key(KeyEvent.KEYCODE_GRAVE)

        
        val LeftBracket = Key(KeyEvent.KEYCODE_LEFT_BRACKET)

        
        val RightBracket = Key(KeyEvent.KEYCODE_RIGHT_BRACKET)

        
        val Slash = Key(KeyEvent.KEYCODE_SLASH)

        
        val Backslash = Key(KeyEvent.KEYCODE_BACKSLASH)

        
        val Semicolon = Key(KeyEvent.KEYCODE_SEMICOLON)

        
        val Apostrophe = Key(KeyEvent.KEYCODE_APOSTROPHE)

        
        val At = Key(KeyEvent.KEYCODE_AT)

        
        val Number = Key(KeyEvent.KEYCODE_NUM)

        
        val HeadsetHook = Key(KeyEvent.KEYCODE_HEADSETHOOK)

        
        val Focus = Key(KeyEvent.KEYCODE_FOCUS)

        
        val Menu = Key(KeyEvent.KEYCODE_MENU)

        
        val Notification = Key(KeyEvent.KEYCODE_NOTIFICATION)

        
        val Search = Key(KeyEvent.KEYCODE_SEARCH)

        
        val PageUp = Key(KeyEvent.KEYCODE_PAGE_UP)

        
        val PageDown = Key(KeyEvent.KEYCODE_PAGE_DOWN)

        
        val PictureSymbols = Key(KeyEvent.KEYCODE_PICTSYMBOLS)

        
        val SwitchCharset = Key(KeyEvent.KEYCODE_SWITCH_CHARSET)

        
        val ButtonA = Key(KeyEvent.KEYCODE_BUTTON_A)

        
        val ButtonB = Key(KeyEvent.KEYCODE_BUTTON_B)

        
        val ButtonC = Key(KeyEvent.KEYCODE_BUTTON_C)

        
        val ButtonX = Key(KeyEvent.KEYCODE_BUTTON_X)

        
        val ButtonY = Key(KeyEvent.KEYCODE_BUTTON_Y)

        
        val ButtonZ = Key(KeyEvent.KEYCODE_BUTTON_Z)

        
        val ButtonL1 = Key(KeyEvent.KEYCODE_BUTTON_L1)

        
        val ButtonR1 = Key(KeyEvent.KEYCODE_BUTTON_R1)

        
        val ButtonL2 = Key(KeyEvent.KEYCODE_BUTTON_L2)

        
        val ButtonR2 = Key(KeyEvent.KEYCODE_BUTTON_R2)

        
        val ButtonThumbLeft = Key(KeyEvent.KEYCODE_BUTTON_THUMBL)

        
        val ButtonThumbRight = Key(KeyEvent.KEYCODE_BUTTON_THUMBR)

        
        val ButtonStart = Key(KeyEvent.KEYCODE_BUTTON_START)

        
        val ButtonSelect = Key(KeyEvent.KEYCODE_BUTTON_SELECT)

        
        val ButtonMode = Key(KeyEvent.KEYCODE_BUTTON_MODE)

        
        val Button1 = Key(KeyEvent.KEYCODE_BUTTON_1)

        
        val Button2 = Key(KeyEvent.KEYCODE_BUTTON_2)

        
        val Button3 = Key(KeyEvent.KEYCODE_BUTTON_3)

        
        val Button4 = Key(KeyEvent.KEYCODE_BUTTON_4)

        
        val Button5 = Key(KeyEvent.KEYCODE_BUTTON_5)

        
        val Button6 = Key(KeyEvent.KEYCODE_BUTTON_6)

        
        val MediaRecord = Key(KeyEvent.KEYCODE_MEDIA_RECORD)

        
        val MediaNext = Key(KeyEvent.KEYCODE_MEDIA_NEXT)

        
        val MediaPrevious = Key(KeyEvent.KEYCODE_MEDIA_PREVIOUS)

        
        val MediaRewind = Key(KeyEvent.KEYCODE_MEDIA_REWIND)

        
        val MediaFastForward = Key(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)

        
        val MediaClose = Key(KeyEvent.KEYCODE_MEDIA_CLOSE)

        
        val MediaAudioTrack = Key(KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK)

        
        val MediaEject = Key(KeyEvent.KEYCODE_MEDIA_EJECT)

        
        val MediaTopMenu = Key(KeyEvent.KEYCODE_MEDIA_TOP_MENU)

        
        val MediaSkipForward = Key(KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD)

        
        val MediaSkipBackward = Key(KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD)

        
        val MediaStepForward = Key(KeyEvent.KEYCODE_MEDIA_STEP_FORWARD)

        
        val MediaStepBackward = Key(KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD)

        
        val MicrophoneMute = Key(KeyEvent.KEYCODE_MUTE)

        
        val VolumeMute = Key(KeyEvent.KEYCODE_VOLUME_MUTE)

        
        val Info = Key(KeyEvent.KEYCODE_INFO)

        
        val ChannelUp = Key(KeyEvent.KEYCODE_CHANNEL_UP)

        
        val ChannelDown = Key(KeyEvent.KEYCODE_CHANNEL_DOWN)

        
        val ZoomIn = Key(KeyEvent.KEYCODE_ZOOM_IN)

        
        val ZoomOut = Key(KeyEvent.KEYCODE_ZOOM_OUT)

        
        val Tv = Key(KeyEvent.KEYCODE_TV)

        
        val Window = Key(KeyEvent.KEYCODE_WINDOW)

        
        val Guide = Key(KeyEvent.KEYCODE_GUIDE)

        
        val Dvr = Key(KeyEvent.KEYCODE_DVR)

        
        val Bookmark = Key(KeyEvent.KEYCODE_BOOKMARK)

        
        val Captions = Key(KeyEvent.KEYCODE_CAPTIONS)

        
        val Settings = Key(KeyEvent.KEYCODE_SETTINGS)

        
        val TvPower = Key(KeyEvent.KEYCODE_TV_POWER)

        
        val TvInput = Key(KeyEvent.KEYCODE_TV_INPUT)

        
        val SetTopBoxPower = Key(KeyEvent.KEYCODE_STB_POWER)

        
        val SetTopBoxInput = Key(KeyEvent.KEYCODE_STB_INPUT)

        
        val AvReceiverPower = Key(KeyEvent.KEYCODE_AVR_POWER)

        
        val AvReceiverInput = Key(KeyEvent.KEYCODE_AVR_INPUT)

        
        val ProgramRed = Key(KeyEvent.KEYCODE_PROG_RED)

        
        val ProgramGreen = Key(KeyEvent.KEYCODE_PROG_GREEN)

        
        val ProgramYellow = Key(KeyEvent.KEYCODE_PROG_YELLOW)

        
        val ProgramBlue = Key(KeyEvent.KEYCODE_PROG_BLUE)

        
        val AppSwitch = Key(KeyEvent.KEYCODE_APP_SWITCH)

        
        val LanguageSwitch = Key(KeyEvent.KEYCODE_LANGUAGE_SWITCH)

        
        val MannerMode = Key(KeyEvent.KEYCODE_MANNER_MODE)

        
        val Toggle2D3D = Key(KeyEvent.KEYCODE_3D_MODE)

        
        val Contacts = Key(KeyEvent.KEYCODE_CONTACTS)

        
        val Calendar = Key(KeyEvent.KEYCODE_CALENDAR)

        
        val Music = Key(KeyEvent.KEYCODE_MUSIC)

        
        val Calculator = Key(KeyEvent.KEYCODE_CALCULATOR)

        
        val ZenkakuHankaru = Key(KeyEvent.KEYCODE_ZENKAKU_HANKAKU)

        
        val Eisu = Key(KeyEvent.KEYCODE_EISU)

        
        val Muhenkan = Key(KeyEvent.KEYCODE_MUHENKAN)

        
        val Henkan = Key(KeyEvent.KEYCODE_HENKAN)

        
        val KatakanaHiragana = Key(KeyEvent.KEYCODE_KATAKANA_HIRAGANA)

        
        val Yen = Key(KeyEvent.KEYCODE_YEN)

        
        val Ro = Key(KeyEvent.KEYCODE_RO)

        
        val Kana = Key(KeyEvent.KEYCODE_KANA)

        
        val Assist = Key(KeyEvent.KEYCODE_ASSIST)

        
        val BrightnessDown = Key(KeyEvent.KEYCODE_BRIGHTNESS_DOWN)

        
        val BrightnessUp = Key(KeyEvent.KEYCODE_BRIGHTNESS_UP)

        
        val Sleep = Key(KeyEvent.KEYCODE_SLEEP)

        
        val WakeUp = Key(KeyEvent.KEYCODE_WAKEUP)

        
        val SoftSleep = Key(KeyEvent.KEYCODE_SOFT_SLEEP)

        
        val Pairing = Key(KeyEvent.KEYCODE_PAIRING)

        
        val LastChannel = Key(KeyEvent.KEYCODE_LAST_CHANNEL)

        
        val TvDataService = Key(KeyEvent.KEYCODE_TV_DATA_SERVICE)

        
        val VoiceAssist = Key(KeyEvent.KEYCODE_VOICE_ASSIST)

        
        val TvRadioService = Key(KeyEvent.KEYCODE_TV_RADIO_SERVICE)

        
        val TvTeletext = Key(KeyEvent.KEYCODE_TV_TELETEXT)

        
        val TvNumberEntry = Key(KeyEvent.KEYCODE_TV_NUMBER_ENTRY)

        
        val TvTerrestrialAnalog = Key(KeyEvent.KEYCODE_TV_TERRESTRIAL_ANALOG)

        
        val TvTerrestrialDigital = Key(KeyEvent.KEYCODE_TV_TERRESTRIAL_DIGITAL)

        
        val TvSatellite = Key(KeyEvent.KEYCODE_TV_SATELLITE)

        
        val TvSatelliteBs = Key(KeyEvent.KEYCODE_TV_SATELLITE_BS)

        
        val TvSatelliteCs = Key(KeyEvent.KEYCODE_TV_SATELLITE_CS)

        
        val TvSatelliteService = Key(KeyEvent.KEYCODE_TV_SATELLITE_SERVICE)

        
        val TvNetwork = Key(KeyEvent.KEYCODE_TV_NETWORK)

        
        val TvAntennaCable = Key(KeyEvent.KEYCODE_TV_ANTENNA_CABLE)

        
        val TvInputHdmi1 = Key(KeyEvent.KEYCODE_TV_INPUT_HDMI_1)

        
        val TvInputHdmi2 = Key(KeyEvent.KEYCODE_TV_INPUT_HDMI_2)

        
        val TvInputHdmi3 = Key(KeyEvent.KEYCODE_TV_INPUT_HDMI_3)

        
        val TvInputHdmi4 = Key(KeyEvent.KEYCODE_TV_INPUT_HDMI_4)

        
        val TvInputComposite1 = Key(KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_1)

        
        val TvInputComposite2 = Key(KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_2)

        
        val TvInputComponent1 = Key(KeyEvent.KEYCODE_TV_INPUT_COMPONENT_1)

        
        val TvInputComponent2 = Key(KeyEvent.KEYCODE_TV_INPUT_COMPONENT_2)

        
        val TvInputVga1 = Key(KeyEvent.KEYCODE_TV_INPUT_VGA_1)

        
        val TvAudioDescription = Key(KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION)

        
        val TvAudioDescriptionMixingVolumeUp = Key(KEYCODE_TV_AUDIO_DESCRIPTION_MIX_UP)

        
        val TvAudioDescriptionMixingVolumeDown = Key(KEYCODE_TV_AUDIO_DESCRIPTION_MIX_DOWN)

        
        val TvZoomMode = Key(KeyEvent.KEYCODE_TV_ZOOM_MODE)

        
        val TvContentsMenu = Key(KeyEvent.KEYCODE_TV_CONTENTS_MENU)

        
        val TvMediaContextMenu = Key(KeyEvent.KEYCODE_TV_MEDIA_CONTEXT_MENU)

        
        val TvTimerProgramming = Key(KeyEvent.KEYCODE_TV_TIMER_PROGRAMMING)

        
        val StemPrimary = Key(KeyEvent.KEYCODE_STEM_PRIMARY)

        
        val Stem1 = Key(KeyEvent.KEYCODE_STEM_1)

        
        val Stem2 = Key(KeyEvent.KEYCODE_STEM_2)

        
        val Stem3 = Key(KeyEvent.KEYCODE_STEM_3)

        
        val AllApps = Key(KeyEvent.KEYCODE_ALL_APPS)

        
        val Refresh = Key(KeyEvent.KEYCODE_REFRESH)

        
        val ThumbsUp = Key(KeyEvent.KEYCODE_THUMBS_UP)

        
        val ThumbsDown = Key(KeyEvent.KEYCODE_THUMBS_DOWN)

        
        val ProfileSwitch = Key(KeyEvent.KEYCODE_PROFILE_SWITCH)

        val NumLock = Key(KeyEvent.KEYCODE_NUM_LOCK)

        val F1 = Key(KeyEvent.KEYCODE_F1)
        val F2 = Key(KeyEvent.KEYCODE_F2)
        val F3 = Key(KeyEvent.KEYCODE_F3)
        val F4 = Key(KeyEvent.KEYCODE_F4)
        val F5 = Key(KeyEvent.KEYCODE_F5)
        val F6 = Key(KeyEvent.KEYCODE_F6)
        val F7 = Key(KeyEvent.KEYCODE_F7)
        val F8 = Key(KeyEvent.KEYCODE_F8)
        val F9 = Key(KeyEvent.KEYCODE_F9)
        val F10 = Key(KeyEvent.KEYCODE_F10)
        val F11 = Key(KeyEvent.KEYCODE_F11)
        val F12 = Key(KeyEvent.KEYCODE_F12)

        val NumPad0 = Key(KeyEvent.KEYCODE_NUMPAD_0)
        val NumPad1 = Key(KeyEvent.KEYCODE_NUMPAD_1)
        val NumPad2 = Key(KeyEvent.KEYCODE_NUMPAD_2)
        val NumPad3 = Key(KeyEvent.KEYCODE_NUMPAD_3)
        val NumPad4 = Key(KeyEvent.KEYCODE_NUMPAD_4)
        val NumPad5 = Key(KeyEvent.KEYCODE_NUMPAD_5)
        val NumPad6 = Key(KeyEvent.KEYCODE_NUMPAD_6)
        val NumPad7 = Key(KeyEvent.KEYCODE_NUMPAD_7)
        val NumPad8 = Key(KeyEvent.KEYCODE_NUMPAD_8)
        val NumPad9 = Key(KeyEvent.KEYCODE_NUMPAD_9)
        val NumPadDivide = Key(KeyEvent.KEYCODE_NUMPAD_DIVIDE)
        val NumPadMultiply = Key(KeyEvent.KEYCODE_NUMPAD_MULTIPLY)
        val NumPadSubtract = Key(KeyEvent.KEYCODE_NUMPAD_SUBTRACT)
        val NumPadAdd = Key(KeyEvent.KEYCODE_NUMPAD_ADD)
        val NumPadDot = Key(KeyEvent.KEYCODE_NUMPAD_DOT)
        val NumPadComma = Key(KeyEvent.KEYCODE_NUMPAD_COMMA)
        val NumPadEnter = Key(KeyEvent.KEYCODE_NUMPAD_ENTER)
        val NumPadEquals = Key(KeyEvent.KEYCODE_NUMPAD_EQUALS)
        val NumPadLeftParenthesis = Key(KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN)
        val NumPadRightParenthesis = Key(KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN)

        val MediaPlay = Key(KeyEvent.KEYCODE_MEDIA_PLAY)
        val MediaPause = Key(KeyEvent.KEYCODE_MEDIA_PAUSE)
        val MediaStop = Key(KeyEvent.KEYCODE_MEDIA_STOP)

        val Button7 = Key(KeyEvent.KEYCODE_BUTTON_7)
        val Button8 = Key(KeyEvent.KEYCODE_BUTTON_8)
        val Button9 = Key(KeyEvent.KEYCODE_BUTTON_9)
        val Button10 = Key(KeyEvent.KEYCODE_BUTTON_10)
        val Button11 = Key(KeyEvent.KEYCODE_BUTTON_11)
        val Button12 = Key(KeyEvent.KEYCODE_BUTTON_12)
        val Button13 = Key(KeyEvent.KEYCODE_BUTTON_13)
        val Button14 = Key(KeyEvent.KEYCODE_BUTTON_14)
        val Button15 = Key(KeyEvent.KEYCODE_BUTTON_15)
        val Button16 = Key(KeyEvent.KEYCODE_BUTTON_16)
        val Forward = Key(KeyEvent.KEYCODE_FORWARD)

        val NumPadDirectionUp = NumPad8
        val NumPadDirectionDown = NumPad2
        val NumPadDirectionLeft = NumPad4
        val NumPadDirectionRight = NumPad6
        val NumPadMoveHome = NumPad7
        val NumPadMoveEnd = NumPad1
        val NumPadPageUp = NumPad9
        val NumPadPageDown = NumPad3
        val NumPadInsert = NumPad0
        val NumPadDelete = NumPadDot
    }

    override fun toString(): String = "Key code: $keyCode"
}

val Key.nativeKeyCode: Int
    get() = keyCode.toInt()

fun Key(nativeKeyCode: Int): Key = Key(nativeKeyCode.toLong())
