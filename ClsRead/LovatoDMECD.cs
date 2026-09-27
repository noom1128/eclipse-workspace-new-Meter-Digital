using MySql.Data.MySqlClient;
using System;
using System.Collections.Generic;
using System.Data;
using System.IO;
using System.IO.Ports;
using System.Linq;
using System.Text;
using System.Threading;
using System.Windows.Forms;
using System.Xml;

namespace TTCReading.Class.ClsRead
{
    class LovatoDMECD
    {
        ClsConnDB ConnDB = new ClsConnDB();
        ClsCRC16 crc16 = new ClsCRC16();
        ClsComPort port = new ClsComPort();
        SerialPort ComPort = new SerialPort();
        string pathmeter = System.IO.Path.GetDirectoryName(System.Reflection.Assembly.GetExecutingAssembly().Location) + "\\ElectricRT\\";
        double[] datameter;
        string meterid = "";
        string meterName = "";
        string MeterAddr = "";
        string energytype = "";
        string CH = "";
        string dis = "";
        string chkerrorcon = "";
        string str = "";
        string[] str1;
        string chkdata = "";
        string por_type = "";

        double[] denominator = new double[9];
        private void setdenominator()
        {
            denominator[0] = 100;//
            denominator[1] = 100;//
            denominator[2] = 100;//
            denominator[3] = 100;//
            denominator[4] = 100;//
            denominator[5] = 100;//
            denominator[6] = 100;//
            denominator[7] = 100;//
            denominator[8] = 100;//
        }

        #region WriteMeter
        public void readmeterDMECD(DataTable tb)
        {
            foreach (DataRow dr in tb.Rows)
            {
                meterid = dr["meter_id"].ToString();
                meterName = dr["meter_name"].ToString();
                MeterAddr = dr["meter_addr"].ToString();
                CH = dr["meter_ch"].ToString();
                energytype = dr["energy_type"].ToString();
                dis = dr["meter_dis"].ToString();
                por_type = dr["por_type"].ToString();
                switch (por_type)
                {
                    case "1":
                        port.cboPorts = dr["por_comport"].ToString();
                        port.cboBaudRate = dr["por_buadrate"].ToString();
                        port.cboDataBits = dr["por_databit"].ToString();
                        port.cboParity = dr["por_parity"].ToString();
                        port.cboStopBits = dr["por_stopbit"].ToString();
                        ComPort = port.connectSP();
                        break;
                    case "2":

                        break;
                    case "3":

                        break;
                }
                try
                {
                    int cloop = 32;
                    double[] MakeDword = new double[9];
                    int c = 0;
                    int[] bytes = { 4, 4, 4, 4, 4, 4, 4, 4, 4 };
                    if (chkerrorcon == "")
                    {
                        str = readdatameter4byte(Convert.ToInt32(MeterAddr), Int32.Parse("100", System.Globalization.NumberStyles.HexNumber));
                        str1 = str.Split(',');
                        for (int i = 0; i < cloop; i += bytes[c])
                        {
                            byte[] ReData = new byte[4];
                            ReData[0] = Convert.ToByte(str1[i + 0]);
                            ReData[1] = Convert.ToByte(str1[i + 1]);
                            ReData[2] = Convert.ToByte(str1[i + 2]);
                            ReData[3] = Convert.ToByte(str1[i + 3]);
                            MakeDword[c] = crc16.Read(ReData);
                            c++;
                        }
                    }
                    MakeDword[8] = Convert.ToInt32(str1[1]);
                    datameter = new double[9];
                    setdenominator();
                    for (int l = 0; l < 9; l++)
                    {
                        datameter[l] = MakeDword[l] / denominator[l];
                    }
                    DMECD();
                    port.closeSP(ComPort);
                }
                catch (Exception) { }
            }
        }
        #endregion WriteMeter

        #region ReadMeter
        private string readdatameter4byte(int addr, Int64 command)
        {
            string date = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss", new System.Globalization.CultureInfo("en-US"));            
            string valuemeter = "";
            Int64 GethightByte = command / 256;
            Int64 GetlowByte = command % 256;
            byte[] Data = new byte[8];
            int[] crc_result = new int[2];

            Data[0] = (byte)addr;
            Data[1] = 0x03;
            Data[2] = (byte)GethightByte;
            Data[3] = (byte)GetlowByte;
            Data[4] = 0x00;
            Data[5] = 0x16;

            crc_result = crc16.crc16(Data, 6);

            Data[6] = (byte)crc_result[0];
            Data[7] = (byte)crc_result[1];
            // Send the one character buffer.
            ComPort.Write(Data, 0, Data.Length);
            Thread.Sleep(10);

            byte[] Alldata = new byte[36];
            int offset = 0;
            int remaining = Alldata.Length;
            while (remaining > 0)
            {
                try
                {
                    int read = ComPort.Read(Alldata, offset, remaining);
                    if (read <= 0) { remaining = 0; }
                    else
                    {
                        remaining -= read;
                        offset += read;
                    }
                    if (Convert.ToBoolean(dis) == true)
                    {
                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            ConnDB.sql = "UPDATE eg_ms_meter " +
                            "SET meter_dis = @meter_dis, meter_rec_date = @meter_rec_date, meter_rec_status = @meter_rec_status " +
                            "WHERE meter_id = @meter_id ";
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_dis", 0);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_date", date);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_status", "ADD");
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                    }
                }
                catch (Exception) 
                { 
                    chkdata = "";
                    if (Convert.ToBoolean(dis) == false)
                    {
                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            ConnDB.sql = "UPDATE eg_ms_meter " +
                            "SET meter_dis = @meter_dis, meter_rec_date = @meter_rec_date, meter_rec_status = @meter_rec_status " +
                            "WHERE meter_id = @meter_id ";
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_dis", 1);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_date", date);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_rec_status", "ACT");
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                    }
                    break; 
                }
            }
            if (chkdata == "")
            {
                byte[] ReData = new byte[32];
                for (int c = 0; c < 32; c++)
                {
                    ReData[c] = Alldata[c + 3];
                    valuemeter += ReData[c].ToString() + ",";
                }
            }
            return valuemeter;
        }
        #endregion ReadMeter

        #region Save
        private void DMECD()
        {
            try
            {
                DataTable tb = new DataTable();
                tb.Columns.Add("Begin", typeof(string));
                tb.Columns.Add("ID", typeof(string));
                tb.Columns.Add("Name", typeof(string));
                tb.Columns.Add("CH1", typeof(float));
                tb.Columns.Add("CH2", typeof(float));
                tb.Columns.Add("CH3", typeof(float));
                tb.Columns.Add("CH4", typeof(float));
                tb.Columns.Add("CH5", typeof(float));
                tb.Columns.Add("CH6", typeof(float));
                tb.Columns.Add("CH7", typeof(float));
                tb.Columns.Add("CH8", typeof(float));

                string id = DateTime.Now.ToString("yyyyMMddHHmm", new System.Globalization.CultureInfo("en-US"));
                double[] data = datameter;
                DataRow dr = tb.NewRow();
                dr["Begin"] = id;
                dr["ID"] = meterid;
                dr["Name"] = meterName;
                dr["CH1"] = data[0].ToString("0.00");
                dr["CH2"] = data[1].ToString("0.00");
                dr["CH3"] = data[2].ToString("0.00");
                dr["CH4"] = data[3].ToString("0.00");
                dr["CH5"] = data[4].ToString("0.00");
                dr["CH6"] = data[5].ToString("0.00");
                dr["CH7"] = data[6].ToString("0.00");
                dr["CH8"] = data[7].ToString("0.00");
                tb.Rows.Add(dr);
                if (Convert.ToBoolean(dis) == false)
                {
                    SaveRealTime(dr);
                    //Save_1min(dr);
                }
                else
                {
                    ErrorRealTime();
                }
            }
            catch (Exception ex) { MessageBox.Show("ReadMySQL LovatoDMECD" + ex.Message); }
        }

        private bool chkrecord(string tb, string where)
        {
            bool connect = ConnDB.connect();
            bool chk = false;
            if (connect)
            {
                ConnDB.sql = "select * from " + tb + " " + where;
                ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                chk = ConnDB.reader.HasRows;
                ConnDB.reader.Close();
            }
            ConnDB.clossDB();
            return chk;
        }

        private void ErrorRealTime()
        {
            string Time = DateTime.Now.ToString("yyyyMMddHH", new System.Globalization.CultureInfo("en-US"));
            string MeterData = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
            bool chkdemand = chkrecord("eg_tt_data", "where meter_id ='" + meterid + "'");
            bool connect = ConnDB.connect();
            if (connect)
            {
                switch (CH)
                {
                    case "1":
                        string CH1 = MeterData;
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tt_data " +
                            "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tt_data " +
                            "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                            "where meter_id = @meter_id ";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_data", CH1);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    case "2":
                        string CH2 = MeterData;
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tt_data " +
                            "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tt_data " +
                            "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                            "where meter_id = @meter_id ";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_data", CH2);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    case "3":
                        string CH3 = MeterData;
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tt_data " +
                            "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tt_data " +
                            "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                            "where meter_id = @meter_id ";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_data", CH3);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    case "4":
                        string CH4 = MeterData;
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tt_data " +
                            "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tt_data " +
                            "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                            "where meter_id = @meter_id ";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_data", CH4);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    case "5":
                        string CH5 = MeterData;
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tt_data " +
                            "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tt_data " +
                            "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                            "where meter_id = @meter_id ";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_data", CH5);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    case "6":
                        string CH6 = MeterData;
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tt_data " +
                            "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tt_data " +
                            "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                            "where meter_id = @meter_id ";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_data", CH6);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    case "7":
                        string CH7 = MeterData;
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tt_data " +
                            "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tt_data " +
                            "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                            "where meter_id = @meter_id ";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_data", CH7);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    default:
                        string CH8 = MeterData;
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tt_data " +
                            "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tt_data " +
                            "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                            "where meter_id = @meter_id ";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_data", CH8);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                }
            }
            ConnDB.clossDB();
        }

        private void SaveRealTime(DataRow dr)
        {
            string Time = DateTime.Now.ToString("yyyyMMddHH", new System.Globalization.CultureInfo("en-US"));
            bool chkdemand = chkrecord("eg_tt_data", "where meter_id ='" + meterid + "'");
            bool connect = ConnDB.connect();
            if (connect)
            {
                switch (CH)
                {
                    case "1":
                        string CH1 = "0,0,0,0,0," + dr["CH1"] + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tt_data " +
                            "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tt_data " +
                            "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                            "where meter_id = @meter_id ";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_data", CH1);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    case "2":
                        string CH2 = "0,0,0,0,0," + dr["CH2"] + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tt_data " +
                            "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tt_data " +
                            "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                            "where meter_id = @meter_id ";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_data", CH2);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    case "3":
                        string CH3 = "0,0,0,0,0," + dr["CH3"] + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tt_data " +
                            "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tt_data " +
                            "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                            "where meter_id = @meter_id ";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_data", CH3);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    case "4":
                        string CH4 = "0,0,0,0,0," + dr["CH4"] + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tt_data " +
                            "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tt_data " +
                            "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                            "where meter_id = @meter_id ";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_data", CH4);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    case "5":
                        string CH5 = "0,0,0,0,0," + dr["CH5"] + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tt_data " +
                            "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tt_data " +
                            "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                            "where meter_id = @meter_id ";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_data", CH5);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    case "6":
                        string CH6 = "0,0,0,0,0," + dr["CH6"] + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tt_data " +
                            "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tt_data " +
                            "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                            "where meter_id = @meter_id ";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_data", CH6);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    case "7":
                        string CH7 = "0,0,0,0,0," + dr["CH7"] + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tt_data " +
                            "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tt_data " +
                            "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                            "where meter_id = @meter_id ";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_data", CH7);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    default:
                        string CH8 = "0,0,0,0,0," + dr["CH8"] + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tt_data " +
                            "value(@meter_id, @eg_id, @energy_type, @eg_data)";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tt_data " +
                            "set eg_id = @eg_id, energy_type = @energy_type, eg_data = @eg_data " +
                            "where meter_id = @meter_id ";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.mycomm.Parameters.AddWithValue("@eg_data", CH8);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                }
            }
            ConnDB.clossDB();
        }

        private void Save_1min(DataRow dr)
        {
            DateTime now = DateTime.Now, date = new DateTime(now.Year, now.Month, now.Day, now.Hour, now.Minute, 0);
            string Time = date.ToString("yyyyMMddHH", new System.Globalization.CultureInfo("en-US"));
            string mm = date.ToString("mm", new System.Globalization.CultureInfo("en-US"));
            bool chkdemand = chkrecord("eg_tr_minutes", "where hour_id='" + Time + "' and meter_id ='" + meterid + "'");

            string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
            string[] eg = new string[60];
            eg[00] = value; eg[01] = value; eg[02] = value; eg[03] = value; eg[04] = value; eg[05] = value; eg[06] = value; eg[07] = value; eg[08] = value; eg[09] = value;
            eg[10] = value; eg[11] = value; eg[12] = value; eg[13] = value; eg[14] = value; eg[15] = value; eg[16] = value; eg[17] = value; eg[18] = value; eg[19] = value;
            eg[20] = value; eg[21] = value; eg[22] = value; eg[23] = value; eg[24] = value; eg[25] = value; eg[26] = value; eg[27] = value; eg[28] = value; eg[29] = value;
            eg[30] = value; eg[31] = value; eg[32] = value; eg[33] = value; eg[34] = value; eg[35] = value; eg[36] = value; eg[37] = value; eg[38] = value; eg[39] = value;
            eg[40] = value; eg[41] = value; eg[42] = value; eg[43] = value; eg[44] = value; eg[45] = value; eg[46] = value; eg[47] = value; eg[48] = value; eg[49] = value;
            eg[50] = value; eg[51] = value; eg[52] = value; eg[53] = value; eg[54] = value; eg[55] = value; eg[56] = value; eg[57] = value; eg[58] = value; eg[59] = value;

            bool connect = ConnDB.connect();
            if (connect)
            {
                switch (CH)
                {
                    case "1":
                        string CH1 = "0,0,0,0,0," + dr["CH1"] + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tr_minutes " +
                            "values (@hour_id,@meter_id,@energy_type,'" +
                            eg[00] + "','" + eg[01] + "','" + eg[02] + "','" + eg[03] + "','" + eg[04] + "','" + eg[05] + "','" + eg[06] + "','" + eg[07] + "','" + eg[08] + "','" + eg[09] + "','" +
                            eg[10] + "','" + eg[11] + "','" + eg[12] + "','" + eg[13] + "','" + eg[14] + "','" + eg[15] + "','" + eg[16] + "','" + eg[17] + "','" + eg[18] + "','" + eg[19] + "','" +
                            eg[20] + "','" + eg[21] + "','" + eg[22] + "','" + eg[23] + "','" + eg[24] + "','" + eg[25] + "','" + eg[26] + "','" + eg[27] + "','" + eg[28] + "','" + eg[29] + "','" +
                            eg[30] + "','" + eg[31] + "','" + eg[32] + "','" + eg[33] + "','" + eg[34] + "','" + eg[35] + "','" + eg[36] + "','" + eg[37] + "','" + eg[38] + "','" + eg[39] + "','" +
                            eg[40] + "','" + eg[41] + "','" + eg[42] + "','" + eg[43] + "','" + eg[44] + "','" + eg[45] + "','" + eg[46] + "','" + eg[47] + "','" + eg[48] + "','" + eg[49] + "','" +
                            eg[50] + "','" + eg[51] + "','" + eg[52] + "','" + eg[53] + "','" + eg[54] + "','" + eg[55] + "','" + eg[56] + "','" + eg[57] + "','" + eg[58] + "','" + eg[59] + "');";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tr_minutes set eg_" + mm + "='" + CH1 + "' where hour_id=@hour_id and meter_id=@meter_id";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@hour_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    case "2":
                        string CH2 = "0,0,0,0,0," + dr["CH2"] + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tr_minutes " +
                            "values (@hour_id,@meter_id,@energy_type,'" +
                            eg[00] + "','" + eg[01] + "','" + eg[02] + "','" + eg[03] + "','" + eg[04] + "','" + eg[05] + "','" + eg[06] + "','" + eg[07] + "','" + eg[08] + "','" + eg[09] + "','" +
                            eg[10] + "','" + eg[11] + "','" + eg[12] + "','" + eg[13] + "','" + eg[14] + "','" + eg[15] + "','" + eg[16] + "','" + eg[17] + "','" + eg[18] + "','" + eg[19] + "','" +
                            eg[20] + "','" + eg[21] + "','" + eg[22] + "','" + eg[23] + "','" + eg[24] + "','" + eg[25] + "','" + eg[26] + "','" + eg[27] + "','" + eg[28] + "','" + eg[29] + "','" +
                            eg[30] + "','" + eg[31] + "','" + eg[32] + "','" + eg[33] + "','" + eg[34] + "','" + eg[35] + "','" + eg[36] + "','" + eg[37] + "','" + eg[38] + "','" + eg[39] + "','" +
                            eg[40] + "','" + eg[41] + "','" + eg[42] + "','" + eg[43] + "','" + eg[44] + "','" + eg[45] + "','" + eg[46] + "','" + eg[47] + "','" + eg[48] + "','" + eg[49] + "','" +
                            eg[50] + "','" + eg[51] + "','" + eg[52] + "','" + eg[53] + "','" + eg[54] + "','" + eg[55] + "','" + eg[56] + "','" + eg[57] + "','" + eg[58] + "','" + eg[59] + "');";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tr_minutes set eg_" + mm + "='" + CH2 + "' where hour_id=@hour_id and meter_id=@meter_id";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@hour_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    case "3":
                        string CH3 = "0,0,0,0,0," + dr["CH3"] + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tr_minutes " +
                            "values (@hour_id,@meter_id,@energy_type,'" +
                            eg[00] + "','" + eg[01] + "','" + eg[02] + "','" + eg[03] + "','" + eg[04] + "','" + eg[05] + "','" + eg[06] + "','" + eg[07] + "','" + eg[08] + "','" + eg[09] + "','" +
                            eg[10] + "','" + eg[11] + "','" + eg[12] + "','" + eg[13] + "','" + eg[14] + "','" + eg[15] + "','" + eg[16] + "','" + eg[17] + "','" + eg[18] + "','" + eg[19] + "','" +
                            eg[20] + "','" + eg[21] + "','" + eg[22] + "','" + eg[23] + "','" + eg[24] + "','" + eg[25] + "','" + eg[26] + "','" + eg[27] + "','" + eg[28] + "','" + eg[29] + "','" +
                            eg[30] + "','" + eg[31] + "','" + eg[32] + "','" + eg[33] + "','" + eg[34] + "','" + eg[35] + "','" + eg[36] + "','" + eg[37] + "','" + eg[38] + "','" + eg[39] + "','" +
                            eg[40] + "','" + eg[41] + "','" + eg[42] + "','" + eg[43] + "','" + eg[44] + "','" + eg[45] + "','" + eg[46] + "','" + eg[47] + "','" + eg[48] + "','" + eg[49] + "','" +
                            eg[50] + "','" + eg[51] + "','" + eg[52] + "','" + eg[53] + "','" + eg[54] + "','" + eg[55] + "','" + eg[56] + "','" + eg[57] + "','" + eg[58] + "','" + eg[59] + "');";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tr_minutes set eg_" + mm + "='" + CH3 + "' where hour_id=@hour_id and meter_id=@meter_id";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@hour_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    case "4":
                        string CH4 = "0,0,0,0,0," + dr["CH4"] + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tr_minutes " +
                            "values (@hour_id,@meter_id,@energy_type,'" +
                            eg[00] + "','" + eg[01] + "','" + eg[02] + "','" + eg[03] + "','" + eg[04] + "','" + eg[05] + "','" + eg[06] + "','" + eg[07] + "','" + eg[08] + "','" + eg[09] + "','" +
                            eg[10] + "','" + eg[11] + "','" + eg[12] + "','" + eg[13] + "','" + eg[14] + "','" + eg[15] + "','" + eg[16] + "','" + eg[17] + "','" + eg[18] + "','" + eg[19] + "','" +
                            eg[20] + "','" + eg[21] + "','" + eg[22] + "','" + eg[23] + "','" + eg[24] + "','" + eg[25] + "','" + eg[26] + "','" + eg[27] + "','" + eg[28] + "','" + eg[29] + "','" +
                            eg[30] + "','" + eg[31] + "','" + eg[32] + "','" + eg[33] + "','" + eg[34] + "','" + eg[35] + "','" + eg[36] + "','" + eg[37] + "','" + eg[38] + "','" + eg[39] + "','" +
                            eg[40] + "','" + eg[41] + "','" + eg[42] + "','" + eg[43] + "','" + eg[44] + "','" + eg[45] + "','" + eg[46] + "','" + eg[47] + "','" + eg[48] + "','" + eg[49] + "','" +
                            eg[50] + "','" + eg[51] + "','" + eg[52] + "','" + eg[53] + "','" + eg[54] + "','" + eg[55] + "','" + eg[56] + "','" + eg[57] + "','" + eg[58] + "','" + eg[59] + "');";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tr_minutes set eg_" + mm + "='" + CH4 + "' where hour_id=@hour_id and meter_id=@meter_id";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@hour_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    case "5":
                        string CH5 = "0,0,0,0,0," + dr["CH5"] + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tr_minutes " +
                            "values (@hour_id,@meter_id,@energy_type,'" +
                            eg[00] + "','" + eg[01] + "','" + eg[02] + "','" + eg[03] + "','" + eg[04] + "','" + eg[05] + "','" + eg[06] + "','" + eg[07] + "','" + eg[08] + "','" + eg[09] + "','" +
                            eg[10] + "','" + eg[11] + "','" + eg[12] + "','" + eg[13] + "','" + eg[14] + "','" + eg[15] + "','" + eg[16] + "','" + eg[17] + "','" + eg[18] + "','" + eg[19] + "','" +
                            eg[20] + "','" + eg[21] + "','" + eg[22] + "','" + eg[23] + "','" + eg[24] + "','" + eg[25] + "','" + eg[26] + "','" + eg[27] + "','" + eg[28] + "','" + eg[29] + "','" +
                            eg[30] + "','" + eg[31] + "','" + eg[32] + "','" + eg[33] + "','" + eg[34] + "','" + eg[35] + "','" + eg[36] + "','" + eg[37] + "','" + eg[38] + "','" + eg[39] + "','" +
                            eg[40] + "','" + eg[41] + "','" + eg[42] + "','" + eg[43] + "','" + eg[44] + "','" + eg[45] + "','" + eg[46] + "','" + eg[47] + "','" + eg[48] + "','" + eg[49] + "','" +
                            eg[50] + "','" + eg[51] + "','" + eg[52] + "','" + eg[53] + "','" + eg[54] + "','" + eg[55] + "','" + eg[56] + "','" + eg[57] + "','" + eg[58] + "','" + eg[59] + "');";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tr_minutes set eg_" + mm + "='" + CH5 + "' where hour_id=@hour_id and meter_id=@meter_id";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@hour_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    case "6":
                        string CH6 = "0,0,0,0,0," + dr["CH6"] + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tr_minutes " +
                            "values (@hour_id,@meter_id,@energy_type,'" +
                            eg[00] + "','" + eg[01] + "','" + eg[02] + "','" + eg[03] + "','" + eg[04] + "','" + eg[05] + "','" + eg[06] + "','" + eg[07] + "','" + eg[08] + "','" + eg[09] + "','" +
                            eg[10] + "','" + eg[11] + "','" + eg[12] + "','" + eg[13] + "','" + eg[14] + "','" + eg[15] + "','" + eg[16] + "','" + eg[17] + "','" + eg[18] + "','" + eg[19] + "','" +
                            eg[20] + "','" + eg[21] + "','" + eg[22] + "','" + eg[23] + "','" + eg[24] + "','" + eg[25] + "','" + eg[26] + "','" + eg[27] + "','" + eg[28] + "','" + eg[29] + "','" +
                            eg[30] + "','" + eg[31] + "','" + eg[32] + "','" + eg[33] + "','" + eg[34] + "','" + eg[35] + "','" + eg[36] + "','" + eg[37] + "','" + eg[38] + "','" + eg[39] + "','" +
                            eg[40] + "','" + eg[41] + "','" + eg[42] + "','" + eg[43] + "','" + eg[44] + "','" + eg[45] + "','" + eg[46] + "','" + eg[47] + "','" + eg[48] + "','" + eg[49] + "','" +
                            eg[50] + "','" + eg[51] + "','" + eg[52] + "','" + eg[53] + "','" + eg[54] + "','" + eg[55] + "','" + eg[56] + "','" + eg[57] + "','" + eg[58] + "','" + eg[59] + "');";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tr_minutes set eg_" + mm + "='" + CH6 + "' where hour_id=@hour_id and meter_id=@meter_id";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@hour_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    case "7":
                        string CH7 = "0,0,0,0,0," + dr["CH7"] + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tr_minutes " +
                            "values (@hour_id,@meter_id,@energy_type,'" +
                            eg[00] + "','" + eg[01] + "','" + eg[02] + "','" + eg[03] + "','" + eg[04] + "','" + eg[05] + "','" + eg[06] + "','" + eg[07] + "','" + eg[08] + "','" + eg[09] + "','" +
                            eg[10] + "','" + eg[11] + "','" + eg[12] + "','" + eg[13] + "','" + eg[14] + "','" + eg[15] + "','" + eg[16] + "','" + eg[17] + "','" + eg[18] + "','" + eg[19] + "','" +
                            eg[20] + "','" + eg[21] + "','" + eg[22] + "','" + eg[23] + "','" + eg[24] + "','" + eg[25] + "','" + eg[26] + "','" + eg[27] + "','" + eg[28] + "','" + eg[29] + "','" +
                            eg[30] + "','" + eg[31] + "','" + eg[32] + "','" + eg[33] + "','" + eg[34] + "','" + eg[35] + "','" + eg[36] + "','" + eg[37] + "','" + eg[38] + "','" + eg[39] + "','" +
                            eg[40] + "','" + eg[41] + "','" + eg[42] + "','" + eg[43] + "','" + eg[44] + "','" + eg[45] + "','" + eg[46] + "','" + eg[47] + "','" + eg[48] + "','" + eg[49] + "','" +
                            eg[50] + "','" + eg[51] + "','" + eg[52] + "','" + eg[53] + "','" + eg[54] + "','" + eg[55] + "','" + eg[56] + "','" + eg[57] + "','" + eg[58] + "','" + eg[59] + "');";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tr_minutes set eg_" + mm + "='" + CH7 + "' where hour_id=@hour_id and meter_id=@meter_id";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@hour_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                    default:
                        string CH8 = "0,0,0,0,0," + dr["CH8"] + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        if (!chkdemand)
                        {
                            ConnDB.sql = "insert into eg_tr_minutes " +
                            "values (@hour_id,@meter_id,@energy_type,'" +
                            eg[00] + "','" + eg[01] + "','" + eg[02] + "','" + eg[03] + "','" + eg[04] + "','" + eg[05] + "','" + eg[06] + "','" + eg[07] + "','" + eg[08] + "','" + eg[09] + "','" +
                            eg[10] + "','" + eg[11] + "','" + eg[12] + "','" + eg[13] + "','" + eg[14] + "','" + eg[15] + "','" + eg[16] + "','" + eg[17] + "','" + eg[18] + "','" + eg[19] + "','" +
                            eg[20] + "','" + eg[21] + "','" + eg[22] + "','" + eg[23] + "','" + eg[24] + "','" + eg[25] + "','" + eg[26] + "','" + eg[27] + "','" + eg[28] + "','" + eg[29] + "','" +
                            eg[30] + "','" + eg[31] + "','" + eg[32] + "','" + eg[33] + "','" + eg[34] + "','" + eg[35] + "','" + eg[36] + "','" + eg[37] + "','" + eg[38] + "','" + eg[39] + "','" +
                            eg[40] + "','" + eg[41] + "','" + eg[42] + "','" + eg[43] + "','" + eg[44] + "','" + eg[45] + "','" + eg[46] + "','" + eg[47] + "','" + eg[48] + "','" + eg[49] + "','" +
                            eg[50] + "','" + eg[51] + "','" + eg[52] + "','" + eg[53] + "','" + eg[54] + "','" + eg[55] + "','" + eg[56] + "','" + eg[57] + "','" + eg[58] + "','" + eg[59] + "');";
                        }
                        else
                        {
                            ConnDB.sql = "update eg_tr_minutes set eg_" + mm + "='" + CH8 + "' where hour_id=@hour_id and meter_id=@meter_id";
                        }
                        ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                        ConnDB.mycomm.Parameters.AddWithValue("@hour_id", Time);
                        ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                        ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                        ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                        ConnDB.reader.Close();
                        break;
                }
            }
            ConnDB.clossDB();
        }

        private void Save_15min(DataTable tb)
        {
            DateTime now = DateTime.Now, date = new DateTime(now.Year, now.Month, now.Day, now.Hour, 0, 0);
            DateTime minute = date.AddMinutes((now.Minute / 15) * 15);
            string Time = minute.ToString("yyyyMMddHH", new System.Globalization.CultureInfo("en-US"));
            string mm = minute.ToString("mm", new System.Globalization.CultureInfo("en-US"));
            string time_begin = minute.AddMinutes(-15).ToString("yyyyMMddHHmm", new System.Globalization.CultureInfo("en-US"));
            string time_final = minute.ToString("yyyyMMddHHmm", new System.Globalization.CultureInfo("en-US"));
            bool chkdemand = chkrecord("eg_tr_demand", "where hour_id='" + Time + "' and meter_id ='" + meterid + "'");
            DataRow[] begin, final;
            final = tb.Select("Begin = " + time_final);
            double unit = 0;

            switch (CH)
            {
                case "1":
                    foreach (DataRow dr in final)
                    {
                        begin = tb.Select("Begin =" + time_begin + "AND ID = " + dr[1]);
                        double kwh_begin = begin.Length != 0 ? Convert.ToDouble(begin[0]["CH1"]) : Convert.ToDouble(dr["CH1"]);
                        unit = Convert.ToDouble(dr["CH1"]) - kwh_begin;

                        string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        string[] eg = new string[4];
                        eg[0] = value; eg[1] = value; eg[2] = value; eg[3] = value;

                        string data = "0,0,0,0,0," + kwh_begin.ToString("0.00") + "," + dr["CH1"] + "," + unit + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";

                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            if (!chkdemand)
                            {
                                ConnDB.sql = "insert into eg_tr_demand " +
                                "values (@hour_id,@meter_id,@energy_type,'" + eg[0] + "','" + eg[1] + "','" + eg[2] + "','" + eg[3] + "');";
                            }
                            else
                            {
                                ConnDB.sql = "update eg_tr_demand set eg_" + mm + "='" + data + "' where hour_id=@hour_id and meter_id=@meter_id";
                            }
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@hour_id", Time);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                    }
                    break;
                case "2":
                    foreach (DataRow dr in final)
                    {
                        begin = tb.Select("Begin =" + time_begin + "AND ID = " + dr[1]);
                        double kwh_begin = begin.Length != 0 ? Convert.ToDouble(begin[0]["CH2"]) : Convert.ToDouble(dr["CH2"]);
                        unit = Convert.ToDouble(dr["CH2"]) - kwh_begin;

                        string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        string[] eg = new string[4];
                        eg[0] = value; eg[1] = value; eg[2] = value; eg[3] = value;

                        string data = "0,0,0,0,0," + kwh_begin.ToString("0.00") + "," + dr["CH2"] + "," + unit + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";

                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            if (!chkdemand)
                            {
                                ConnDB.sql = "insert into eg_tr_demand " +
                                "values (@hour_id,@meter_id,@energy_type,'" + eg[0] + "','" + eg[1] + "','" + eg[2] + "','" + eg[3] + "');";
                            }
                            else
                            {
                                ConnDB.sql = "update eg_tr_demand set eg_" + mm + "='" + data + "' where hour_id=@hour_id and meter_id=@meter_id";
                            }
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@hour_id", Time);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                    }
                    break;
                case "3":
                    foreach (DataRow dr in final)
                    {
                        begin = tb.Select("Begin =" + time_begin + "AND ID = " + dr[1]);
                        double kwh_begin = begin.Length != 0 ? Convert.ToDouble(begin[0]["CH3"]) : Convert.ToDouble(dr["CH3"]);
                        unit = Convert.ToDouble(dr["CH3"]) - kwh_begin;

                        string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        string[] eg = new string[4];
                        eg[0] = value; eg[1] = value; eg[2] = value; eg[3] = value;

                        string data = "0,0,0,0,0," + kwh_begin.ToString("0.00") + "," + dr["CH3"] + "," + unit + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";

                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            if (!chkdemand)
                            {
                                ConnDB.sql = "insert into eg_tr_demand " +
                                "values (@hour_id,@meter_id,@energy_type,'" + eg[0] + "','" + eg[1] + "','" + eg[2] + "','" + eg[3] + "');";
                            }
                            else
                            {
                                ConnDB.sql = "update eg_tr_demand set eg_" + mm + "='" + data + "' where hour_id=@hour_id and meter_id=@meter_id";
                            }
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@hour_id", Time);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                    }
                    break;
                case "4":
                    foreach (DataRow dr in final)
                    {
                        begin = tb.Select("Begin =" + time_begin + "AND ID = " + dr[1]);
                        double kwh_begin = begin.Length != 0 ? Convert.ToDouble(begin[0]["CH4"]) : Convert.ToDouble(dr["CH4"]);
                        unit = Convert.ToDouble(dr["CH4"]) - kwh_begin;

                        string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        string[] eg = new string[4];
                        eg[0] = value; eg[1] = value; eg[2] = value; eg[3] = value;

                        string data = "0,0,0,0,0," + kwh_begin.ToString("0.00") + "," + dr["CH4"] + "," + unit + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";

                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            if (!chkdemand)
                            {
                                ConnDB.sql = "insert into eg_tr_demand " +
                                "values (@hour_id,@meter_id,@energy_type,'" + eg[0] + "','" + eg[1] + "','" + eg[2] + "','" + eg[3] + "');";
                            }
                            else
                            {
                                ConnDB.sql = "update eg_tr_demand set eg_" + mm + "='" + data + "' where hour_id=@hour_id and meter_id=@meter_id";
                            }
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@hour_id", Time);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                    }
                    break;
                case "5":
                    foreach (DataRow dr in final)
                    {
                        begin = tb.Select("Begin =" + time_begin + "AND ID = " + dr[1]);
                        double kwh_begin = begin.Length != 0 ? Convert.ToDouble(begin[0]["CH5"]) : Convert.ToDouble(dr["CH5"]);
                        unit = Convert.ToDouble(dr["CH5"]) - kwh_begin;

                        string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        string[] eg = new string[4];
                        eg[0] = value; eg[1] = value; eg[2] = value; eg[3] = value;

                        string data = "0,0,0,0,0," + kwh_begin.ToString("0.00") + "," + dr["CH5"] + "," + unit + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";

                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            if (!chkdemand)
                            {
                                ConnDB.sql = "insert into eg_tr_demand " +
                                "values (@hour_id,@meter_id,@energy_type,'" + eg[0] + "','" + eg[1] + "','" + eg[2] + "','" + eg[3] + "');";
                            }
                            else
                            {
                                ConnDB.sql = "update eg_tr_demand set eg_" + mm + "='" + data + "' where hour_id=@hour_id and meter_id=@meter_id";
                            }
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@hour_id", Time);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                    }
                    break;
                case "6":
                    foreach (DataRow dr in final)
                    {
                        begin = tb.Select("Begin =" + time_begin + "AND ID = " + dr[1]);
                        double kwh_begin = begin.Length != 0 ? Convert.ToDouble(begin[0]["CH6"]) : Convert.ToDouble(dr["CH6"]);
                        unit = Convert.ToDouble(dr["CH6"]) - kwh_begin;

                        string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        string[] eg = new string[4];
                        eg[0] = value; eg[1] = value; eg[2] = value; eg[3] = value;

                        string data = "0,0,0,0,0," + kwh_begin.ToString("0.00") + "," + dr["CH6"] + "," + unit + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";

                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            if (!chkdemand)
                            {
                                ConnDB.sql = "insert into eg_tr_demand " +
                                "values (@hour_id,@meter_id,@energy_type,'" + eg[0] + "','" + eg[1] + "','" + eg[2] + "','" + eg[3] + "');";
                            }
                            else
                            {
                                ConnDB.sql = "update eg_tr_demand set eg_" + mm + "='" + data + "' where hour_id=@hour_id and meter_id=@meter_id";
                            }
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@hour_id", Time);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                    }
                    break;
                case "7":
                    foreach (DataRow dr in final)
                    {
                        begin = tb.Select("Begin =" + time_begin + "AND ID = " + dr[1]);
                        double kwh_begin = begin.Length != 0 ? Convert.ToDouble(begin[0]["CH7"]) : Convert.ToDouble(dr["CH7"]);
                        unit = Convert.ToDouble(dr["CH7"]) - kwh_begin;

                        string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        string[] eg = new string[4];
                        eg[0] = value; eg[1] = value; eg[2] = value; eg[3] = value;

                        string data = "0,0,0,0,0," + kwh_begin.ToString("0.00") + "," + dr["CH7"] + "," + unit + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";

                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            if (!chkdemand)
                            {
                                ConnDB.sql = "insert into eg_tr_demand " +
                                "values (@hour_id,@meter_id,@energy_type,'" + eg[0] + "','" + eg[1] + "','" + eg[2] + "','" + eg[3] + "');";
                            }
                            else
                            {
                                ConnDB.sql = "update eg_tr_demand set eg_" + mm + "='" + data + "' where hour_id=@hour_id and meter_id=@meter_id";
                            }
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@hour_id", Time);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                    }
                    break;
                default:
                    foreach (DataRow dr in final)
                    {
                        begin = tb.Select("Begin =" + time_begin + "AND ID = " + dr[1]);
                        double kwh_begin = begin.Length != 0 ? Convert.ToDouble(begin[0]["CH8"]) : Convert.ToDouble(dr["CH8"]);
                        unit = Convert.ToDouble(dr["CH8"]) - kwh_begin;

                        string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        string[] eg = new string[4];
                        eg[0] = value; eg[1] = value; eg[2] = value; eg[3] = value;

                        string data = "0,0,0,0,0," + kwh_begin.ToString("0.00") + "," + dr["CH8"] + "," + unit + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";

                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            if (!chkdemand)
                            {
                                ConnDB.sql = "insert into eg_tr_demand " +
                                "values (@hour_id,@meter_id,@energy_type,'" + eg[0] + "','" + eg[1] + "','" + eg[2] + "','" + eg[3] + "');";
                            }
                            else
                            {
                                ConnDB.sql = "update eg_tr_demand set eg_" + mm + "='" + data + "' where hour_id=@hour_id and meter_id=@meter_id";
                            }
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@hour_id", Time);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                    }
                    break;
            }
        }

        private void Save_1hour(DataTable tb)
        {
            DateTime now = DateTime.Now;
            DateTime date = new DateTime(now.Year, now.Month, now.Day, now.Hour, 0, 0);
            string Time = date.ToString("yyyyMMdd", new System.Globalization.CultureInfo("en-US"));
            string HH = date.ToString("HH", new System.Globalization.CultureInfo("en-US"));
            string time_begin = date.AddHours(-1).ToString("yyyyMMddHHmm", new System.Globalization.CultureInfo("en-US"));
            string time_final = date.ToString("yyyyMMddHHmm", new System.Globalization.CultureInfo("en-US"));
            bool chkdemand = chkrecord("eg_tr_hour", "where day_id='" + Time + "' and meter_id ='" + meterid + "'");
            DataRow[] begin, final;
            final = tb.Select("Begin = " + time_final);
            double unit = 0;
            switch (CH)
            {
                case "1":
                    foreach (DataRow dr in final)
                    {
                        begin = tb.Select("Begin >=" + time_begin + "AND Begin < " + time_final + "AND ID = " + dr[1]);
                        double kwh_begin = begin.Length != 0 ? Convert.ToDouble(begin[0]["CH1"]) : Convert.ToDouble(dr["CH1"]);
                        unit = Convert.ToDouble(dr["CH1"]) - kwh_begin;

                        string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        string[] eg = new string[24];
                        eg[0] = value; eg[1] = value; eg[2] = value; eg[3] = value; eg[4] = value; eg[5] = value;
                        eg[6] = value; eg[7] = value; eg[8] = value; eg[9] = value; eg[10] = value; eg[11] = value;
                        eg[12] = value; eg[13] = value; eg[14] = value; eg[15] = value; eg[16] = value; eg[17] = value;
                        eg[18] = value; eg[19] = value; eg[20] = value; eg[21] = value; eg[22] = value; eg[23] = value;

                        string data = "0,0,0,0,0," + kwh_begin.ToString("0.00") + "," + dr["CH1"] + "," + unit + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";

                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            if (!chkdemand)
                            {
                                ConnDB.sql = "insert into eg_tr_hour " +
                                "values (@day_id,@meter_id,@energy_type,'" +
                                eg[0] + "','" + eg[1] + "','" + eg[2] + "','" + eg[3] + "','" + eg[4] + "','" + eg[5] + "','" + eg[6] + "','" + eg[7] + "','" +
                                eg[8] + "','" + eg[9] + "','" + eg[10] + "','" + eg[11] + "','" + eg[12] + "','" + eg[13] + "','" + eg[14] + "','" + eg[15] + "','" +
                                eg[16] + "','" + eg[17] + "','" + eg[18] + "','" + eg[19] + "','" + eg[20] + "','" + eg[21] + "','" + eg[22] + "','" + eg[23] + "');";
                            }
                            else
                            {
                                ConnDB.sql = "update eg_tr_hour set eg_" + HH + "='" + data + "' where day_id=@day_id and meter_id=@meter_id";
                            }
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@day_id", Time);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                    }
                    break;
                case "2":
                    foreach (DataRow dr in final)
                    {
                        begin = tb.Select("Begin >=" + time_begin + "AND Begin < " + time_final + "AND ID = " + dr[1]);
                        double kwh_begin = begin.Length != 0 ? Convert.ToDouble(begin[0]["CH2"]) : Convert.ToDouble(dr["CH2"]);
                        unit = Convert.ToDouble(dr["CH2"]) - kwh_begin;

                        string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        string[] eg = new string[24];
                        eg[0] = value; eg[1] = value; eg[2] = value; eg[3] = value; eg[4] = value; eg[5] = value;
                        eg[6] = value; eg[7] = value; eg[8] = value; eg[9] = value; eg[10] = value; eg[11] = value;
                        eg[12] = value; eg[13] = value; eg[14] = value; eg[15] = value; eg[16] = value; eg[17] = value;
                        eg[18] = value; eg[19] = value; eg[20] = value; eg[21] = value; eg[22] = value; eg[23] = value;

                        string data = "0,0,0,0,0," + kwh_begin.ToString("0.00") + "," + dr["CH2"] + "," + unit + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";

                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            if (!chkdemand)
                            {
                                ConnDB.sql = "insert into eg_tr_hour " +
                                "values (@day_id,@meter_id,@energy_type,'" +
                                eg[0] + "','" + eg[1] + "','" + eg[2] + "','" + eg[3] + "','" + eg[4] + "','" + eg[5] + "','" + eg[6] + "','" + eg[7] + "','" +
                                eg[8] + "','" + eg[9] + "','" + eg[10] + "','" + eg[11] + "','" + eg[12] + "','" + eg[13] + "','" + eg[14] + "','" + eg[15] + "','" +
                                eg[16] + "','" + eg[17] + "','" + eg[18] + "','" + eg[19] + "','" + eg[20] + "','" + eg[21] + "','" + eg[22] + "','" + eg[23] + "');";
                            }
                            else
                            {
                                ConnDB.sql = "update eg_tr_hour set eg_" + HH + "='" + data + "' where day_id=@day_id and meter_id=@meter_id";
                            }
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@day_id", Time);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                    }
                    break;
                case "3":
                    foreach (DataRow dr in final)
                    {
                        begin = tb.Select("Begin >=" + time_begin + "AND Begin < " + time_final + "AND ID = " + dr[1]);
                        double kwh_begin = begin.Length != 0 ? Convert.ToDouble(begin[0]["CH3"]) : Convert.ToDouble(dr["CH3"]);
                        unit = Convert.ToDouble(dr["CH3"]) - kwh_begin;

                        string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        string[] eg = new string[24];
                        eg[0] = value; eg[1] = value; eg[2] = value; eg[3] = value; eg[4] = value; eg[5] = value;
                        eg[6] = value; eg[7] = value; eg[8] = value; eg[9] = value; eg[10] = value; eg[11] = value;
                        eg[12] = value; eg[13] = value; eg[14] = value; eg[15] = value; eg[16] = value; eg[17] = value;
                        eg[18] = value; eg[19] = value; eg[20] = value; eg[21] = value; eg[22] = value; eg[23] = value;

                        string data = "0,0,0,0,0," + kwh_begin.ToString("0.00") + "," + dr["CH3"] + "," + unit + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";

                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            if (!chkdemand)
                            {
                                ConnDB.sql = "insert into eg_tr_hour " +
                                "values (@day_id,@meter_id,@energy_type,'" +
                                eg[0] + "','" + eg[1] + "','" + eg[2] + "','" + eg[3] + "','" + eg[4] + "','" + eg[5] + "','" + eg[6] + "','" + eg[7] + "','" +
                                eg[8] + "','" + eg[9] + "','" + eg[10] + "','" + eg[11] + "','" + eg[12] + "','" + eg[13] + "','" + eg[14] + "','" + eg[15] + "','" +
                                eg[16] + "','" + eg[17] + "','" + eg[18] + "','" + eg[19] + "','" + eg[20] + "','" + eg[21] + "','" + eg[22] + "','" + eg[23] + "');";
                            }
                            else
                            {
                                ConnDB.sql = "update eg_tr_hour set eg_" + HH + "='" + data + "' where day_id=@day_id and meter_id=@meter_id";
                            }
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@day_id", Time);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                    }
                    break;
                case "4":
                    foreach (DataRow dr in final)
                    {
                        begin = tb.Select("Begin >=" + time_begin + "AND Begin < " + time_final + "AND ID = " + dr[1]);
                        double kwh_begin = begin.Length != 0 ? Convert.ToDouble(begin[0]["CH4"]) : Convert.ToDouble(dr["CH4"]);
                        unit = Convert.ToDouble(dr["CH4"]) - kwh_begin;

                        string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        string[] eg = new string[24];
                        eg[0] = value; eg[1] = value; eg[2] = value; eg[3] = value; eg[4] = value; eg[5] = value;
                        eg[6] = value; eg[7] = value; eg[8] = value; eg[9] = value; eg[10] = value; eg[11] = value;
                        eg[12] = value; eg[13] = value; eg[14] = value; eg[15] = value; eg[16] = value; eg[17] = value;
                        eg[18] = value; eg[19] = value; eg[20] = value; eg[21] = value; eg[22] = value; eg[23] = value;

                        string data = "0,0,0,0,0," + kwh_begin.ToString("0.00") + "," + dr["CH4"] + "," + unit + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";

                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            if (!chkdemand)
                            {
                                ConnDB.sql = "insert into eg_tr_hour " +
                                "values (@day_id,@meter_id,@energy_type,'" +
                                eg[0] + "','" + eg[1] + "','" + eg[2] + "','" + eg[3] + "','" + eg[4] + "','" + eg[5] + "','" + eg[6] + "','" + eg[7] + "','" +
                                eg[8] + "','" + eg[9] + "','" + eg[10] + "','" + eg[11] + "','" + eg[12] + "','" + eg[13] + "','" + eg[14] + "','" + eg[15] + "','" +
                                eg[16] + "','" + eg[17] + "','" + eg[18] + "','" + eg[19] + "','" + eg[20] + "','" + eg[21] + "','" + eg[22] + "','" + eg[23] + "');";
                            }
                            else
                            {
                                ConnDB.sql = "update eg_tr_hour set eg_" + HH + "='" + data + "' where day_id=@day_id and meter_id=@meter_id";
                            }
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@day_id", Time);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                    }
                    break;
                case "5":
                    foreach (DataRow dr in final)
                    {
                        begin = tb.Select("Begin >=" + time_begin + "AND Begin < " + time_final + "AND ID = " + dr[1]);
                        var minVal = begin.Min(p => p["CH5"]);
                        double kwh_begin = begin.Length != 0 ? Convert.ToDouble(begin[0]["CH5"]) : Convert.ToDouble(dr["CH5"]);
                        unit = Convert.ToDouble(dr["CH5"]) - kwh_begin;

                        string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        string[] eg = new string[24];
                        eg[0] = value; eg[1] = value; eg[2] = value; eg[3] = value; eg[4] = value; eg[5] = value;
                        eg[6] = value; eg[7] = value; eg[8] = value; eg[9] = value; eg[10] = value; eg[11] = value;
                        eg[12] = value; eg[13] = value; eg[14] = value; eg[15] = value; eg[16] = value; eg[17] = value;
                        eg[18] = value; eg[19] = value; eg[20] = value; eg[21] = value; eg[22] = value; eg[23] = value;

                        string data = "0,0,0,0,0," + kwh_begin.ToString("0.00") + "," + dr["CH5"] + "," + unit + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";

                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            if (!chkdemand)
                            {
                                ConnDB.sql = "insert into eg_tr_hour " +
                                "values (@day_id,@meter_id,@energy_type,'" +
                                eg[0] + "','" + eg[1] + "','" + eg[2] + "','" + eg[3] + "','" + eg[4] + "','" + eg[5] + "','" + eg[6] + "','" + eg[7] + "','" +
                                eg[8] + "','" + eg[9] + "','" + eg[10] + "','" + eg[11] + "','" + eg[12] + "','" + eg[13] + "','" + eg[14] + "','" + eg[15] + "','" +
                                eg[16] + "','" + eg[17] + "','" + eg[18] + "','" + eg[19] + "','" + eg[20] + "','" + eg[21] + "','" + eg[22] + "','" + eg[23] + "');";
                            }
                            else
                            {
                                ConnDB.sql = "update eg_tr_hour set eg_" + HH + "='" + data + "' where day_id=@day_id and meter_id=@meter_id";
                            }
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@day_id", Time);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                    }
                    break;
                case "6":
                    foreach (DataRow dr in final)
                    {
                        begin = tb.Select("Begin >=" + time_begin + "AND Begin < " + time_final + "AND ID = " + dr[1]);
                        double kwh_begin = begin.Length != 0 ? Convert.ToDouble(begin[0]["CH6"]) : Convert.ToDouble(dr["CH6"]);
                        unit = Convert.ToDouble(dr["CH6"]) - kwh_begin;

                        string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        string[] eg = new string[24];
                        eg[0] = value; eg[1] = value; eg[2] = value; eg[3] = value; eg[4] = value; eg[5] = value;
                        eg[6] = value; eg[7] = value; eg[8] = value; eg[9] = value; eg[10] = value; eg[11] = value;
                        eg[12] = value; eg[13] = value; eg[14] = value; eg[15] = value; eg[16] = value; eg[17] = value;
                        eg[18] = value; eg[19] = value; eg[20] = value; eg[21] = value; eg[22] = value; eg[23] = value;

                        string data = "0,0,0,0,0," + kwh_begin.ToString("0.00") + "," + dr["CH6"] + "," + unit + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";

                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            if (!chkdemand)
                            {
                                ConnDB.sql = "insert into eg_tr_hour " +
                                "values (@day_id,@meter_id,@energy_type,'" +
                                eg[0] + "','" + eg[1] + "','" + eg[2] + "','" + eg[3] + "','" + eg[4] + "','" + eg[5] + "','" + eg[6] + "','" + eg[7] + "','" +
                                eg[8] + "','" + eg[9] + "','" + eg[10] + "','" + eg[11] + "','" + eg[12] + "','" + eg[13] + "','" + eg[14] + "','" + eg[15] + "','" +
                                eg[16] + "','" + eg[17] + "','" + eg[18] + "','" + eg[19] + "','" + eg[20] + "','" + eg[21] + "','" + eg[22] + "','" + eg[23] + "');";
                            }
                            else
                            {
                                ConnDB.sql = "update eg_tr_hour set eg_" + HH + "='" + data + "' where day_id=@day_id and meter_id=@meter_id";
                            }
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@day_id", Time);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                    }
                    break;
                case "7":
                    foreach (DataRow dr in final)
                    {
                        begin = tb.Select("Begin >=" + time_begin + "AND Begin < " + time_final + "AND ID = " + dr[1]);
                        double kwh_begin = begin.Length != 0 ? Convert.ToDouble(begin[0]["CH7"]) : Convert.ToDouble(dr["CH7"]);
                        unit = Convert.ToDouble(dr["CH7"]) - kwh_begin;

                        string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        string[] eg = new string[24];
                        eg[0] = value; eg[1] = value; eg[2] = value; eg[3] = value; eg[4] = value; eg[5] = value;
                        eg[6] = value; eg[7] = value; eg[8] = value; eg[9] = value; eg[10] = value; eg[11] = value;
                        eg[12] = value; eg[13] = value; eg[14] = value; eg[15] = value; eg[16] = value; eg[17] = value;
                        eg[18] = value; eg[19] = value; eg[20] = value; eg[21] = value; eg[22] = value; eg[23] = value;

                        string data = "0,0,0,0,0," + kwh_begin.ToString("0.00") + "," + dr["CH7"] + "," + unit + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";

                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            if (!chkdemand)
                            {
                                ConnDB.sql = "insert into eg_tr_hour " +
                                "values (@day_id,@meter_id,@energy_type,'" +
                                eg[0] + "','" + eg[1] + "','" + eg[2] + "','" + eg[3] + "','" + eg[4] + "','" + eg[5] + "','" + eg[6] + "','" + eg[7] + "','" +
                                eg[8] + "','" + eg[9] + "','" + eg[10] + "','" + eg[11] + "','" + eg[12] + "','" + eg[13] + "','" + eg[14] + "','" + eg[15] + "','" +
                                eg[16] + "','" + eg[17] + "','" + eg[18] + "','" + eg[19] + "','" + eg[20] + "','" + eg[21] + "','" + eg[22] + "','" + eg[23] + "');";
                            }
                            else
                            {
                                ConnDB.sql = "update eg_tr_hour set eg_" + HH + "='" + data + "' where day_id=@day_id and meter_id=@meter_id";
                            }
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@day_id", Time);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                    }
                    break;
                default:
                    foreach (DataRow dr in final)
                    {
                        begin = tb.Select("Begin >=" + time_begin + "AND Begin < " + time_final + "AND ID = " + dr[1]);
                        double kwh_begin = begin.Length != 0 ? Convert.ToDouble(begin[0]["CH8"]) : Convert.ToDouble(dr["CH8"]);
                        unit = Convert.ToDouble(dr["CH8"]) - kwh_begin;

                        string value = "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";
                        string[] eg = new string[24];
                        eg[0] = value; eg[1] = value; eg[2] = value; eg[3] = value; eg[4] = value; eg[5] = value;
                        eg[6] = value; eg[7] = value; eg[8] = value; eg[9] = value; eg[10] = value; eg[11] = value;
                        eg[12] = value; eg[13] = value; eg[14] = value; eg[15] = value; eg[16] = value; eg[17] = value;
                        eg[18] = value; eg[19] = value; eg[20] = value; eg[21] = value; eg[22] = value; eg[23] = value;

                        string data = "0,0,0,0,0," + kwh_begin.ToString("0.00") + "," + dr["CH8"] + "," + unit + ",0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0";

                        bool connect = ConnDB.connect();
                        if (connect)
                        {
                            if (!chkdemand)
                            {
                                ConnDB.sql = "insert into eg_tr_hour " +
                                "values (@day_id,@meter_id,@energy_type,'" +
                                eg[0] + "','" + eg[1] + "','" + eg[2] + "','" + eg[3] + "','" + eg[4] + "','" + eg[5] + "','" + eg[6] + "','" + eg[7] + "','" +
                                eg[8] + "','" + eg[9] + "','" + eg[10] + "','" + eg[11] + "','" + eg[12] + "','" + eg[13] + "','" + eg[14] + "','" + eg[15] + "','" +
                                eg[16] + "','" + eg[17] + "','" + eg[18] + "','" + eg[19] + "','" + eg[20] + "','" + eg[21] + "','" + eg[22] + "','" + eg[23] + "');";
                            }
                            else
                            {
                                ConnDB.sql = "update eg_tr_hour set eg_" + HH + "='" + data + "' where day_id=@day_id and meter_id=@meter_id";
                            }
                            ConnDB.mycomm = new MySqlCommand(ConnDB.sql, ConnDB.myconn);
                            ConnDB.mycomm.Parameters.AddWithValue("@day_id", Time);
                            ConnDB.mycomm.Parameters.AddWithValue("@meter_id", meterid);
                            ConnDB.mycomm.Parameters.AddWithValue("@energy_type", energytype);
                            ConnDB.reader = ConnDB.mycomm.ExecuteReader();
                            ConnDB.reader.Close();
                        }
                        ConnDB.clossDB();
                    }
                    break;
            }
        }

        private void XMLDME_CD()
        {
            if (File.Exists(pathmeter + meterName + ".xml") == false)
            {
                XmlTextWriter writer = new XmlTextWriter(pathmeter + meterName + ".xml", System.Text.Encoding.UTF8);
                writer.WriteStartDocument(true);
                writer.Formatting = Formatting.Indented;
                writer.Indentation = 2;
                writer.WriteStartElement("ReadMeter");
                DMECD(writer);
                writer.WriteEndElement();
                writer.WriteEndDocument();
                writer.Close();
            }
            else { UpdateDMECD(); }
        }

        private void DMECD(XmlTextWriter writer)
        {
            try
            {
                double[] data = datameter;
                switch (CH)
                {
                    case "1":
                        writer.WriteStartElement("DateTime");
                        writer.WriteString(DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss", new System.Globalization.CultureInfo("en-US")));
                        writer.WriteEndElement();
                        writer.WriteStartElement("MeterID");
                        writer.WriteString(meterid);
                        writer.WriteEndElement();
                        writer.WriteStartElement("Name");
                        writer.WriteString(meterName);
                        writer.WriteEndElement();
                        writer.WriteStartElement("Address");
                        writer.WriteString(MeterAddr);
                        writer.WriteEndElement();
                        writer.WriteStartElement("EnergyType");
                        writer.WriteString(energytype);
                        writer.WriteEndElement();
                        writer.WriteStartElement("CH1");
                        writer.WriteString(data[0].ToString("0.00"));
                        writer.WriteEndElement();
                        break;
                    case "2":
                        writer.WriteStartElement("DateTime");
                        writer.WriteString(DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss", new System.Globalization.CultureInfo("en-US")));
                        writer.WriteEndElement();
                        writer.WriteStartElement("MeterID");
                        writer.WriteString(meterid);
                        writer.WriteEndElement();
                        writer.WriteStartElement("Name");
                        writer.WriteString(meterName);
                        writer.WriteEndElement();
                        writer.WriteStartElement("Address");
                        writer.WriteString(MeterAddr);
                        writer.WriteEndElement();
                        writer.WriteStartElement("EnergyType");
                        writer.WriteString(energytype);
                        writer.WriteEndElement();
                        writer.WriteStartElement("CH2");
                        writer.WriteString(data[1].ToString("0.00"));
                        writer.WriteEndElement();
                        break;
                    case "3":
                        writer.WriteStartElement("DateTime");
                        writer.WriteString(DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss", new System.Globalization.CultureInfo("en-US")));
                        writer.WriteEndElement();
                        writer.WriteStartElement("MeterID");
                        writer.WriteString(meterid);
                        writer.WriteEndElement();
                        writer.WriteStartElement("Name");
                        writer.WriteString(meterName);
                        writer.WriteEndElement();
                        writer.WriteStartElement("Address");
                        writer.WriteString(MeterAddr);
                        writer.WriteEndElement();
                        writer.WriteStartElement("EnergyType");
                        writer.WriteString(energytype);
                        writer.WriteEndElement();
                        writer.WriteStartElement("CH3");
                        writer.WriteString(data[2].ToString("0.00"));
                        writer.WriteEndElement();
                        break;
                    case "4":
                        writer.WriteStartElement("DateTime");
                        writer.WriteString(DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss", new System.Globalization.CultureInfo("en-US")));
                        writer.WriteEndElement();
                        writer.WriteStartElement("MeterID");
                        writer.WriteString(meterid);
                        writer.WriteEndElement();
                        writer.WriteStartElement("Name");
                        writer.WriteString(meterName);
                        writer.WriteEndElement();
                        writer.WriteStartElement("Address");
                        writer.WriteString(MeterAddr);
                        writer.WriteEndElement();
                        writer.WriteStartElement("EnergyType");
                        writer.WriteString(energytype);
                        writer.WriteEndElement();
                        writer.WriteStartElement("CH4");
                        writer.WriteString(data[3].ToString("0.00"));
                        writer.WriteEndElement();
                        break;
                    case "5":
                        writer.WriteStartElement("DateTime");
                        writer.WriteString(DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss", new System.Globalization.CultureInfo("en-US")));
                        writer.WriteEndElement();
                        writer.WriteStartElement("MeterID");
                        writer.WriteString(meterid);
                        writer.WriteEndElement();
                        writer.WriteStartElement("Name");
                        writer.WriteString(meterName);
                        writer.WriteEndElement();
                        writer.WriteStartElement("Address");
                        writer.WriteString(MeterAddr);
                        writer.WriteEndElement();
                        writer.WriteStartElement("EnergyType");
                        writer.WriteString(energytype);
                        writer.WriteEndElement();
                        writer.WriteStartElement("CH5");
                        writer.WriteString(data[4].ToString("0.00"));
                        writer.WriteEndElement();
                        break;
                    case "6":
                        writer.WriteStartElement("DateTime");
                        writer.WriteString(DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss", new System.Globalization.CultureInfo("en-US")));
                        writer.WriteEndElement();
                        writer.WriteStartElement("MeterID");
                        writer.WriteString(meterid);
                        writer.WriteEndElement();
                        writer.WriteStartElement("Name");
                        writer.WriteString(meterName);
                        writer.WriteEndElement();
                        writer.WriteStartElement("Address");
                        writer.WriteString(MeterAddr);
                        writer.WriteEndElement();
                        writer.WriteStartElement("EnergyType");
                        writer.WriteString(energytype);
                        writer.WriteEndElement();
                        writer.WriteStartElement("CH6");
                        writer.WriteString(data[5].ToString("0.00"));
                        writer.WriteEndElement();
                        break;
                    case "7":
                        writer.WriteStartElement("DateTime");
                        writer.WriteString(DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss", new System.Globalization.CultureInfo("en-US")));
                        writer.WriteEndElement();
                        writer.WriteStartElement("MeterID");
                        writer.WriteString(meterid);
                        writer.WriteEndElement();
                        writer.WriteStartElement("Name");
                        writer.WriteString(meterName);
                        writer.WriteEndElement();
                        writer.WriteStartElement("Address");
                        writer.WriteString(MeterAddr);
                        writer.WriteEndElement();
                        writer.WriteStartElement("EnergyType");
                        writer.WriteString(energytype);
                        writer.WriteEndElement();
                        writer.WriteStartElement("CH7");
                        writer.WriteString(data[6].ToString("0.00"));
                        writer.WriteEndElement();
                        break;
                    default:
                        writer.WriteStartElement("DateTime");
                        writer.WriteString(DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss", new System.Globalization.CultureInfo("en-US")));
                        writer.WriteEndElement();
                        writer.WriteStartElement("MeterID");
                        writer.WriteString(meterid);
                        writer.WriteEndElement();
                        writer.WriteStartElement("Name");
                        writer.WriteString(meterName);
                        writer.WriteEndElement();
                        writer.WriteStartElement("Address");
                        writer.WriteString(MeterAddr);
                        writer.WriteEndElement();
                        writer.WriteStartElement("EnergyType");
                        writer.WriteString(energytype);
                        writer.WriteEndElement();
                        writer.WriteStartElement("CH8");
                        writer.WriteString(data[7].ToString("0.00"));
                        writer.WriteEndElement();
                        break;
                }
            }
            catch (Exception) { }
        }

        private void UpdateDMECD()
        {
            try
            {
                double[] data = datameter;
                string pathdatameter = pathmeter + meterName + ".xml";
                DataSet ds = new DataSet();
                ds.ReadXml(pathdatameter);
                switch (CH)
                {
                    case "1":
                        ds.Tables[0].Rows[0]["DateTime"] = DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
                        ds.Tables[0].Rows[0]["MeterID"] = meterid;
                        ds.Tables[0].Rows[0]["Name"] = meterName;
                        ds.Tables[0].Rows[0]["Address"] = MeterAddr;
                        ds.Tables[0].Rows[0]["EnergyType"] = energytype;
                        ds.Tables[0].Rows[0]["CH1"] = data[0].ToString("0.00");
                        break;
                    case "2":
                        ds.Tables[0].Rows[0]["DateTime"] = DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
                        ds.Tables[0].Rows[0]["MeterID"] = meterid;
                        ds.Tables[0].Rows[0]["Name"] = meterName;
                        ds.Tables[0].Rows[0]["Address"] = MeterAddr;
                        ds.Tables[0].Rows[0]["EnergyType"] = energytype;
                        ds.Tables[0].Rows[0]["CH2"] = data[1].ToString("0.00");
                        break;
                    case "3":
                        ds.Tables[0].Rows[0]["DateTime"] = DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
                        ds.Tables[0].Rows[0]["MeterID"] = meterid;
                        ds.Tables[0].Rows[0]["Name"] = meterName;
                        ds.Tables[0].Rows[0]["Address"] = MeterAddr;
                        ds.Tables[0].Rows[0]["EnergyType"] = energytype;
                        ds.Tables[0].Rows[0]["CH3"] = data[2].ToString("0.00");
                        break;
                    case "4":
                        ds.Tables[0].Rows[0]["DateTime"] = DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
                        ds.Tables[0].Rows[0]["MeterID"] = meterid;
                        ds.Tables[0].Rows[0]["Name"] = meterName;
                        ds.Tables[0].Rows[0]["Address"] = MeterAddr;
                        ds.Tables[0].Rows[0]["EnergyType"] = energytype;
                        ds.Tables[0].Rows[0]["CH4"] = data[3].ToString("0.00");
                        break;
                    case "5":
                        ds.Tables[0].Rows[0]["DateTime"] = DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
                        ds.Tables[0].Rows[0]["MeterID"] = meterid;
                        ds.Tables[0].Rows[0]["Name"] = meterName;
                        ds.Tables[0].Rows[0]["Address"] = MeterAddr;
                        ds.Tables[0].Rows[0]["EnergyType"] = energytype;
                        ds.Tables[0].Rows[0]["CH5"] = data[4].ToString("0.00");
                        break;
                    case "6":
                        ds.Tables[0].Rows[0]["DateTime"] = DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
                        ds.Tables[0].Rows[0]["MeterID"] = meterid;
                        ds.Tables[0].Rows[0]["Name"] = meterName;
                        ds.Tables[0].Rows[0]["Address"] = MeterAddr;
                        ds.Tables[0].Rows[0]["EnergyType"] = energytype;
                        ds.Tables[0].Rows[0]["CH6"] = data[5].ToString("0.00");
                        break;
                    case "7":
                        ds.Tables[0].Rows[0]["DateTime"] = DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
                        ds.Tables[0].Rows[0]["MeterID"] = meterid;
                        ds.Tables[0].Rows[0]["Name"] = meterName;
                        ds.Tables[0].Rows[0]["Address"] = MeterAddr;
                        ds.Tables[0].Rows[0]["EnergyType"] = energytype;
                        ds.Tables[0].Rows[0]["CH7"] = data[6].ToString("0.00");
                        break;
                    default:
                        ds.Tables[0].Rows[0]["DateTime"] = DateTime.Now.ToString("dd/MM/yyyy HH:mm:ss", new System.Globalization.CultureInfo("en-US"));
                        ds.Tables[0].Rows[0]["MeterID"] = meterid;
                        ds.Tables[0].Rows[0]["Name"] = meterName;
                        ds.Tables[0].Rows[0]["Address"] = MeterAddr;
                        ds.Tables[0].Rows[0]["EnergyType"] = energytype;
                        ds.Tables[0].Rows[0]["CH8"] = data[7].ToString("0.00");
                        break;
                }
                ds.WriteXml(pathdatameter);
            }
            catch (Exception ex) { MessageBox.Show("UpdateXML !!" + ex.Message); }
        }
        #endregion Save
    }
}
