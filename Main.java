

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.jukka.MaxRectsBinPack;
import com.jukka.Rect;

/**
 * @author pengyangjin
 * @version 1.0.0
 * */
public class Main {

	public static void main(String[] args) {
		MaxRectsBinPack packer;
		packer = new MaxRectsBinPack(2048, 2048, false);

		List<Rect> listRect = new ArrayList<>();

		// ��Ӳ�������
		listRect.add(new Rect(0, 0, 102, 102));
		listRect.add(new Rect(0, 0, 548, 166));
		listRect.add(new Rect(0, 0, 102, 102));
		listRect.add(new Rect(0, 0, 550, 618));

		// �Բ������ݰ��������  ��һ����Ҫʡ���� ����ͼ��������
		Collections.sort(listRect, new Comparator<Rect>() {
			@Override
			public int compare(Rect o1, Rect o2) {
				if (o2.width > o1.width) {
					return 1;
				} else if (o2.width < o1.width) {
					return -1;
				} else {
					return 0;
				}
			}
		});

		for (Rect rect : listRect) {
			Rect newRect = packer.Insert(rect.width, rect.height,
					MaxRectsBinPack.FreeRectChoiceHeuristic.RectBestSquareFit);
			if (rect.width == 0) {
				System.out.println("���ʼ������ˡ�");
				break;
			} else {
				System.out.println(newRect.toString());
			}
		}
	}

}
