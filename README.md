![image](https://github.com/skateboard1991/OpenGlCamera/blob/master/show.gif)

# OpenGlCamera
使用opengles用作相机预览，添加视频水印，录制并保存为MP4文件，目前尚不支持音轨。
用法比较简单
    
    RecordActivity.startRecordActivity(context: Context, bitmap: Bitmap, minTime：Floath，totalTime: Float, outputFile: File,waterX:Float=-1f,waterY:Float=1F)
    
    
waterX和waterY是水印左下角坐标的位置，范围为[-1,1]    

# 2018/7/5
加入了音轨，加了部分UI，添加maven引用

# 添加Maven引用方法
# （1）在根项目build.gradle添加：
    allprojects {
		repositories {
		
			maven { url 'https://jitpack.io' }
		}
	}
    
# （2）在项目gradle添加
     dependencies {
	        implementation 'com.github.skateboard1991:OpenGlCamera:1.0.0-snapshot'
	}

