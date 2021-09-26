package com.muazzeznihalbahadir.son_gorev

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.fragment_tarif.*
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.jar.Manifest


class TarifFragment : Fragment() {
    var secilenGorsel: Uri? = null
    var bitmap : Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tarif, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        button.setOnClickListener{
           kaydet(it)
        }
        imageView.setOnClickListener {
            gorselSec(it)
        }
        arguments?.let {
            var gelenBilgi = TarifFragmentArgs.fromBundle(it).bilgi
            if(gelenBilgi.equals("menudengeldim")){
                //yemek kaydetmeye geldi
                yemekIsmiText.setText("")
                yemekMalzemeText.setText("")
                button.visibility = View.VISIBLE //butonu görünür yapar

                var gorselArkaplanSec = BitmapFactory.decodeResource(context?.resources,R.drawable.bildiri)
                imageView.setImageBitmap(gorselArkaplanSec)
            }else{
                //görüntülemeye geldi
                button.visibility = View.INVISIBLE // butonu görünmez yapar
                var secilenId = TarifFragmentArgs.fromBundle(it).id

                context?.let {
                    try {
                        val db = it.openOrCreateDatabase("yemekler",Context.MODE_PRIVATE,null)
                        val cursor = db.rawQuery("SELECT*FROM yemekler WHERE id = ?", arrayOf(secilenId.toString()))

                        val yemekIsmi = cursor.getColumnIndex("yemekismi")
                        val yemekMalzemleri = cursor.getColumnIndex("yemekmalzemesi")
                        val yemekResim = cursor.getColumnIndex("gorsel")

                        while (cursor.moveToNext()){
                            yemekIsmiText.setText(cursor.getString(yemekIsmi))
                            yemekMalzemeText.setText(cursor.getString(yemekMalzemleri))

                            val byteDizisi = cursor.getBlob(yemekResim)
                            val bitmap = BitmapFactory.decodeByteArray(byteDizisi,0,byteDizisi.size)
                            imageView.setImageBitmap(bitmap)
                        }
                        cursor.close()

                    }catch (e : Exception){
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    fun kaydet(view:View){
        val yemekIsmiText = yemekIsmiText.text.toString()
        val yemekMalzemeleriText = yemekMalzemeText.text.toString()

        if (bitmap!=null){
            val kucukBitmap = bitmapSinirlandir(bitmap!!,300)
            val outputSream = ByteArrayOutputStream()
            kucukBitmap.compress(Bitmap.CompressFormat.PNG,50,outputSream)
            val byteDizisi = outputSream.toByteArray()

            try {
                context?.let {
                    val database = it.openOrCreateDatabase("yemekler",Context.MODE_PRIVATE,null)
                    database.execSQL("CREATE TABLE IF  NOT EXISTS yemekler(id INTEGER PRIMARY KEY,yemekismi VARCHAR, yemekmalzemesi VARCHAR , gorsel BLOB)")
                    val sqlString = "INSERT INTO yemekler (yemekismi,yemekmalzemesi,gorsel) VALUES(?,?,?)"
                    val statement = database.compileStatement(sqlString)
                    statement.bindString(1,yemekIsmiText)
                    statement.bindString(2,yemekMalzemeleriText)
                    statement.bindBlob(3,byteDizisi)
                    statement.execute()
                }

            }catch (e : Exception){
                e.printStackTrace()
            }
            val action = TarifFragmentDirections.actionTarifFragmentToListeFragment()
            Navigation.findNavController(view).navigate(action)
        }

    }
    fun gorselSec(view:View){
        activity?.let {
            if(ContextCompat.checkSelfPermission(it.applicationContext,android.Manifest.permission.READ_EXTERNAL_STORAGE )!= PackageManager.PERMISSION_GRANTED) {
                //izin verilmediyse
                requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),1001)
            }else{
                //izin verildiyse
                val galeriIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galeriIntent,1002)
            }
        }


        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        if(requestCode==1001){
            if(grantResults.size>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                val galeriIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galeriIntent,1002)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode==1002 && resultCode== Activity.RESULT_OK && data!=null){
            secilenGorsel = data.data
            try {
                context?.let {
                    if (secilenGorsel !=null ){
                        if (Build.VERSION.SDK_INT >= 28){
                            val source = ImageDecoder.createSource(it.contentResolver,secilenGorsel!!)
                            bitmap = ImageDecoder.decodeBitmap(source)
                            imageView.setImageBitmap(bitmap)
                        }else{
                            bitmap = MediaStore.Images.Media.getBitmap(it.contentResolver,secilenGorsel)
                            imageView.setImageBitmap(bitmap)
                        }
                    }
                }


            }catch (e : Exception){
                e.printStackTrace()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
    fun bitmapSinirlandir(kullaniciBitmap:Bitmap,maxBoyut:Int):Bitmap{
        var width = kullaniciBitmap.width
        var height = kullaniciBitmap.height
        val bitmapOrani : Double = width.toDouble()/height.toDouble()

        if (bitmapOrani>1){
            //resim yatay
            width=maxBoyut
            val kisaltilmisHeight=width/bitmapOrani
            height = kisaltilmisHeight.toInt()
        }else{
            //resim dikey
            height = maxBoyut
            val kisaltilmisWidth = height*bitmapOrani
            width = kisaltilmisWidth.toInt()
        }
        return Bitmap.createScaledBitmap(kullaniciBitmap,width,height,true)
    }
}



